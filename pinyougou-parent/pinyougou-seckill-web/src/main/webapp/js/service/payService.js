app.service('payService',function($http){
	//生成二维码
	this.createNative=function(){
		return $http.get('pay/createNative.do');
	}
	//查询订单支付状态
	this.queryPayStatus=function(outTradeNo){
		return $http.get('pay/queryPayStatus.do?out_trade_no='+outTradeNo);
	}
})