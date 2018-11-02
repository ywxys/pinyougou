app.service('cartService',function($http){
	//获得购物车列表
	this.findCartList=function(){
		return $http.get('cart/findCartList.do');
	}
	//操作购物车item和num
	this.addGoodsToCartList=function(itemId,num){
		return $http.get('cart/addGoodsToCartList.do?itemId='+itemId+'&num='+num);
	}
	//求合计价钱
	this.sum=function(cartList){
		var totalValue={totalNum:0,totalMoney:0};
		for (var i = 0; i < cartList.length; i++) {
			var cart = cartList[i];
			for (var j = 0; j < cart.orderItemList.length; j++) {
				var orderItem = cart.orderItemList[j];
				totalValue.totalMoney += orderItem.totalFee;
				totalValue.totalNum += orderItem.num;
			}
		}
		return totalValue;
	}
})