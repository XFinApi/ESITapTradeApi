/*******************************************************
 * ESITapTradeApiSimpleDemoJavaIdeaIC2018
 * www.xfinapi.com
 *******************************************************/

import java.util.*;

public class Main {
    //////////////////////////////////////////////////////////////////////////////////
    //配置信息
    public static class Config {
		//注册易盛外盘9.0仿真交易账号，http://www.esunny.com.cn/index.php?m=content&c=index&a=lists&catid=50

		//地址
		public String MarketAddress = "123.161.206.213:7171";
		public String TradeAddress = "123.161.206.213:8383";

		//账户
		public String MarketUserName = "ES";
		public String MarketPassword = "123456";
		public String TradeUserName = "Q1203070045";//公用测试账户。为了测试准确，请注册使用您自己的账户。
		public String TradePassword = "a123456";

		//合约
		public String ExchangeID = "COMEX";
		public String ProductID = "GC";
		public String InstrumentID = "1812";

		//行情
		public double SellPrice1 = -1;
		public double BuyPrice1 = -1;
    }

    //////////////////////////////////////////////////////////////////////////////////
    static Config Cfg = new Config();
    static XFinApi.TradeApi.IMarket market;
    static XFinApi.TradeApi.ITrade trade;
    static MarketEvent marketEvent;
    static TradeEvent tradeEvent;

    static void PrintNotifyInfo(XFinApi.TradeApi.NotifyParams param) {
        String strs = "";
        for (int i = 0; i < param.getCodeInfos().size(); i++) {
            XFinApi.TradeApi.CodeInfo info = param.getCodeInfos().get(i);
            strs += "(Code=" + info.getCode() +
                    ";LowerCode=" + info.getLowerCode() +
                    ";LowerMessage=" + info.getLowerMessage() + ")";
        }

        System.out.println(String.format(" OnNotify: Action=%d, Result=%d %s", param.getActionType(), param.getResultType(), strs));
    }

    static void PrintSubscribedInfo(XFinApi.TradeApi.QueryParams instInfo) {
        System.out.println(String.format("- OnSubscribed: %s %s %s", instInfo.getExchangeID(), instInfo.getProductID(), instInfo.getInstrumentID()));
    }

    static void PrintUnsubscribedInfo(XFinApi.TradeApi.QueryParams instInfo) {
        System.out.println(String.format("- OnUnsubscribed: %s %s %s", instInfo.getExchangeID(), instInfo.getProductID(), instInfo.getInstrumentID()));
    }

    static void PrintTickInfo(XFinApi.TradeApi.Tick tick) {
        System.out.println(String.format(" Tick,%s %s %s, HighestPrice=%g, LowestPrice=%g, BidPrice0=%g, BidVolume0=%d, AskPrice0=%g, AskVolume0=%d, LastPrice=%g, LastVolume=%d, TradingTime=%s",
                tick.getExchangeID(),
				tick.getProductID(),
                tick.getInstrumentID(),
                tick.getHighestPrice(),
                tick.getLowestPrice(),
                tick.GetBidPrice(0),
                tick.GetBidVolume(0),
                tick.GetAskPrice(0),
                tick.GetAskVolume(0),
                tick.getLastPrice(),
                tick.getLastVolume(),
                tick.getTradingTime()));
    }

    static void PrintOrderInfo(XFinApi.TradeApi.Order order) {
        System.out.println(String.format("  ProductType=%d, Ref=%s, ID=%s, InstID=%s %s %s, Price=%g, Volume=%d, NoTradedVolume=%d, Direction=%d, OpenCloseType=%d, PriceCond=%d, TimeCond=%d, VolumeCond=%d, Status=%d, Msg=%s, %s",
                order.getProductType().swigValue(),
                order.getOrderRef(), order.getOrderID(),
                order.getExchangeID(), order.getProductID(), order.getInstrumentID(), 
				order.getPrice(), order.getVolume(), order.getNoTradedVolume(),
                order.getDirection().swigValue(), order.getOpenCloseType().swigValue(),
                order.getPriceCond().swigValue(),
                order.getTimeCond().swigValue(),
                order.getVolumeCond().swigValue(),
                order.getStatus().swigValue(),
                order.getStatusMsg(),
                order.getOrderTime()));
    }

    static void PrintTradeInfo(XFinApi.TradeApi.TradeOrder trade) {

        System.out.println(String.format("  ID=%s, OrderID=%s, InstID=%s %s %s, Price=%g, Volume=%d, Direction=%d, OpenCloseType=%d, %s",
                trade.getTradeID(), trade.getOrderID(),
                trade.getExchangeID(), trade.getProductID(), trade.getInstrumentID(), trade.getPrice(), trade.getVolume(),
                trade.getDirection().swigValue(), trade.getOpenCloseType().swigValue(),
                trade.getTradeTime()));
    }

    static void PrintInstrumentInfo(XFinApi.TradeApi.Instrument inst) {
        System.out.println(String.format(" ExchangeID=%s, ProductID=%s, InstrumentID=%s, InstrumentName=%s",
                inst.getExchangeID(), inst.getProductID(),
                inst.getInstrumentID(), inst.getInstrumentName()));
    }

    static void PrintPositionInfo(XFinApi.TradeApi.Position pos) {
        long buyposition = XFinApi.TradeApi.ITradeApi.IsDefaultValue(pos.getBuyPosition()) ? -1 : pos.getBuyPosition();
        long sellposition = XFinApi.TradeApi.ITradeApi.IsDefaultValue(pos.getSellPosition()) ? -1 : pos.getSellPosition();
        long netposition = XFinApi.TradeApi.ITradeApi.IsDefaultValue(pos.getNetPosition()) ? -1 : pos.getNetPosition();
        System.out.println(String.format(" InstID=%s %s %s, BuyPosition=%d, SellPosition=%d, NetPosition=%d",
                pos.getExchangeID(), pos.getProductID(), pos.getInstrumentID(),
                buyposition, sellposition, netposition));
    }

    static void PrintAccountInfo(XFinApi.TradeApi.Account acc) {
        System.out.println(String.format("  Balance=%g, Available=%g, FrozenCommission=%g, FrozenMargin=%g, Commission=%g, MaintenanceMargin=%g, PositionProfit=%g",
                acc.getBalance(), acc.getAvailable(), 
				acc.getFrozenCommission(), acc.getFrozenMargin(), acc.getCommission(),
                acc.getMaintenanceMargin(), acc.getPositionProfit()));
    }

    //////////////////////////////////////////////////////////////////////////////////
    //API 创建失败错误码的含义，其他错误码的含义参见XTA_W32\Cpp\ApiEnum.h文件
    static String[] StrCreateErrors = {
            "无错误",
            "头文件与接口版本不匹配",
            "头文件与实现版本不匹配",
            "实现加载失败",
            "实现入口未找到",
            "创建实例失败",
            "无授权文件",
            "授权版本不符",
            "最后一次通信超限",
            "机器码错误",
            "认证文件到期",
            "认证超时"
    };

    //////////////////////////////////////////////////////////////////////////////////
    //行情事件
    public static class MarketEvent extends XFinApi.TradeApi.MarketListener {
        @Override
        public void OnNotify(XFinApi.TradeApi.NotifyParams notifyParams) {

            System.out.println("* Market");

            PrintNotifyInfo(notifyParams);

            //连接成功后可订阅合约
            if (XFinApi.TradeApi.ActionKind.Open.swigValue() == notifyParams.getActionType() &&
                    XFinApi.TradeApi.ResultKind.Success.swigValue() == notifyParams.getResultType() && market != null) {
                //订阅
                XFinApi.TradeApi.QueryParams param = new XFinApi.TradeApi.QueryParams();
				param.setExchangeID(Cfg.ExchangeID);
				param.setProductID(Cfg.ProductID);
                param.setInstrumentID(Cfg.InstrumentID);
                market.Subscribe(param);
            }

            //ToDo ...
        }

        @Override
        public void OnSubscribed(XFinApi.TradeApi.QueryParams instInfo) {

            PrintSubscribedInfo(instInfo);

            //ToDo ...
        }

        @Override
        public void OnUnsubscribed(XFinApi.TradeApi.QueryParams instInfo) {

            PrintUnsubscribedInfo(instInfo);

            //ToDo ...
        }

        @Override
        public void OnTick(XFinApi.TradeApi.Tick tick) {
            if (Cfg.SellPrice1 <= 0 && Cfg.BuyPrice1 <= 0)

                PrintTickInfo(tick);

            Cfg.SellPrice1 = tick.GetAskPrice(0);
            Cfg.BuyPrice1 = tick.GetBidPrice(0);

            //ToDo ...
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //交易事件

    public static class TradeEvent extends XFinApi.TradeApi.TradeListener {
        @Override
        public void OnNotify(XFinApi.TradeApi.NotifyParams notifyParams) {

            System.out.println("* Trade");

            PrintNotifyInfo(notifyParams);

            //ToDo ...
        }

        @Override
        public void OnUpdateOrder(XFinApi.TradeApi.Order order) {

            System.out.println("- OnUpdateOrder:");

            PrintOrderInfo(order);

            //ToDo ...
        }

        @Override
        public void OnUpdateTradeOrder(XFinApi.TradeApi.TradeOrder trade) {

            System.out.println("- OnUpdateTradeOrder:");

            PrintTradeInfo(trade);

            //ToDo ...
        }

        @Override
        public void OnQueryOrder(XFinApi.TradeApi.OrderList orders) {

            System.out.println("- OnQueryOrder:");

            for (int i = 0; i < orders.size(); i++) {
                XFinApi.TradeApi.Order order = orders.get(i);
                PrintOrderInfo(order);

                //ToDo ...
            }
        }

        @Override
        public void OnQueryTradeOrder(XFinApi.TradeApi.TradeOrderList trades) {

            System.out.println("- OnQueryTradeOrder:");

            for (int i = 0; i < trades.size(); i++) {
                XFinApi.TradeApi.TradeOrder trade = trades.get(i);
                PrintTradeInfo(trade);

                //ToDo ...
            }
        }

        @Override
        public void OnQueryInstrument(XFinApi.TradeApi.InstrumentList insts) {

            System.out.println("- OnQueryInstrument:");

            for (int i = 0; i < insts.size(); i++) {
                XFinApi.TradeApi.Instrument inst = insts.get(i);
                PrintInstrumentInfo(inst);

                //ToDo ...
            }
        }

        @Override
        public void OnQueryPosition(XFinApi.TradeApi.PositionList posInfos) {
            System.out.println("- OnQueryPosition");
            for (int i = 0; i < posInfos.size(); i++) {
                XFinApi.TradeApi.Position pos = posInfos.get(i);
                PrintPositionInfo(pos);
            }

            //ToDo ...
        }

        @Override
        public void OnQueryAccount(XFinApi.TradeApi.Account accInfo) {
            System.out.println("- OnQueryAccount");

            PrintAccountInfo(accInfo);

            //ToDo ...
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //行情测试
    static void MarketTest() {
        //创建 IMarket
        // char* path 指 xxx.exe 同级子目录中的 xxx.dll 文件
        int[] err = new int[1];

        market = XFinApi.TradeApi.ITradeApi.XFinApi_CreateMarketApi("XTA_W32/Api/ESITap_v9.3.0.1_20160406/XFinApi.ESITapTradeApi.dll", err);
        if (err[0] > 0 && market == null) {
            System.out.println(String.format("* Market XFinApiCreateError=%s;", StrCreateErrors[err[0]]));
            return;
        }

        //注册事件
        marketEvent = new MarketEvent();
        market.SetListener(marketEvent);

        //连接服务器
        XFinApi.TradeApi.OpenParams openParams = new XFinApi.TradeApi.OpenParams();
        openParams.setHostAddress(Cfg.MarketAddress);
        openParams.setUserID(Cfg.MarketUserName);
        openParams.setPassword(Cfg.MarketPassword);
        market.Open(openParams);

            /*
            连接成功后才能执行订阅行情等操作，检测方法有两种：
            1、IMarket.IsOpened()=true
            2、MarketListener.OnNotify中
            XFinApi.TradeApi.ActionKind.Open.swigValue() == notifyParams.getActionType() &&
            XFinApi.TradeApi.ResultKind.Success.swigValue() == notifyParams.getResultType()
            */

            /* 行情相关方法
            while (!market.IsOpened())
                 Thread.Sleep(1000);

            //订阅行情，已在MarketEvent.OnNotify中订阅
            XFinApi.TradeApi.QueryParams param = new XFinApi.TradeApi.QueryParams();
			param.setExChangeID(Cfg.ExChangeID);
			param.setProductID(Cfg.ProductID);
			param.setInstrumentID(Cfg.InstrumentID);
			market.Subscribe(param);

            //取消订阅行情
            market.Unsubscribe(param);
            */
    }

    //////////////////////////////////////////////////////////////////////////////////
    //交易测试

    static void TradeTest() {
        //创建 ITrade
        // char* path 指 xxx.exe 同级子目录中的 xxx.dll 文件
        int[] err = new int[1];

        trade = XFinApi.TradeApi.ITradeApi.XFinApi_CreateTradeApi("XTA_W32/Api/ESITap_v9.3.0.1_20160406/XFinApi.ESITapTradeApi.dll", err);

        if (trade == null) {
            System.out.println(String.format("* Trade XFinApiCreateError=%s;", StrCreateErrors[err[0]]));
            return;
        }

        //注册事件
        tradeEvent = new TradeEvent();

        trade.SetListener(tradeEvent);

        //连接服务器
        XFinApi.TradeApi.OpenParams openParams = new XFinApi.TradeApi.OpenParams();
        openParams.setHostAddress(Cfg.TradeAddress);
        openParams.setUserID(Cfg.TradeUserName);
        openParams.setPassword(Cfg.TradePassword);
        openParams.setIsUTF8(true);
        trade.Open(openParams);

            /*
            //连接成功后才能执行查询、委托等操作，检测方法有两种：
            1、ITrade.IsOpened()=true
            2、TradeListener.OnNotify中
            XFinApi.TradeApi.ActionKind.Open.swigValue() == notifyParams.getActionType() &&
            XFinApi.TradeApi.ResultKind.Success.swigValue() == notifyParams.getResultType()
             */
        try {
            while (!trade.IsOpened())
                Thread.sleep(1000);

            XFinApi.TradeApi.QueryParams qryParam = new XFinApi.TradeApi.QueryParams();

            //查询委托单
            Thread.sleep(1000);//有些接口查询有间隔限制，如：CTP查询间隔为1秒
            System.out.println("Press any key to QueryOrder.");
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
            trade.QueryOrder(qryParam);

            //查询成交单
            Thread.sleep(3000);
            System.out.println("Press any key to QueryTradeOrder.");
            scanner.nextLine();
            trade.QueryTradeOrder(qryParam);

            //查询合约
			qryParam.setExchangeID(Cfg.ExchangeID);
			qryParam.setProductID(Cfg.ProductID);
            Thread.sleep(3000);
            System.out.println("Press any key to QueryInstrument.");
            scanner.nextLine();
            trade.QueryInstrument(qryParam);

            //查询持仓
            Thread.sleep(3000);
            System.out.println("Press any key to QueryPosition.");
            scanner.nextLine();
            trade.QueryPosition(qryParam);

            //查询账户
            Thread.sleep(3000);
            System.out.println("Press any key to QueryAccount.");
            scanner.nextLine();
            trade.QueryAccount(qryParam);

            //委托下单
            Thread.sleep(1000);
            System.out.println("Press any key to OrderAction.");
            scanner.nextLine();
            XFinApi.TradeApi.Order order = new XFinApi.TradeApi.Order();
			order.setExchangeID(Cfg.ExchangeID);
			order.setProductID(Cfg.ProductID);
            order.setInstrumentID(Cfg.InstrumentID);
            order.setPrice(Cfg.SellPrice1);
            order.setVolume(1);
            order.setDirection(XFinApi.TradeApi.DirectionKind.Buy);
            order.setOpenCloseType(XFinApi.TradeApi.OpenCloseKind.Open);

            //下单高级选项，可选择性设置
            order.setActionType(XFinApi.TradeApi.OrderActionKind.Insert);//下单
            order.setOrderType(XFinApi.TradeApi.OrderKind.Order);//标准单
            order.setPriceCond(XFinApi.TradeApi.PriceConditionKind.LimitPrice);//限价
            order.setVolumeCond(XFinApi.TradeApi.VolumeConditionKind.AnyVolume);//任意数量
            order.setTimeCond(XFinApi.TradeApi.TimeConditionKind.GFD);//当日有效
            order.setContingentCond(XFinApi.TradeApi.ContingentCondKind.Immediately);//立即
            order.setHedgeType(XFinApi.TradeApi.HedgeKind.Speculation);//投机
            order.setExecResult(XFinApi.TradeApi.ExecResultKind.NoExec);//没有执行

            trade.OrderAction(order);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static {
        System.loadLibrary("XFinApi.ITradeApi.PortJava");
    }

    public static void main(String[] args) {
        MarketTest();
        TradeTest();

        try {
            Scanner scanner = new Scanner(System.in);
            Thread.sleep(2000);
            System.out.println("Press any key to close.");
            scanner.nextLine();

            //关闭连接
            if (market != null) {
                market.Close();
                XFinApi.TradeApi.ITradeApi.XFinApi_ReleaseMarketApi(market);//必须释放资源
            }
            if (trade != null) {
                trade.Close();
                XFinApi.TradeApi.ITradeApi.XFinApi_ReleaseTradeApi(trade);//必须释放资源
            }

            System.out.println("Closed.");
            scanner.nextLine();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
