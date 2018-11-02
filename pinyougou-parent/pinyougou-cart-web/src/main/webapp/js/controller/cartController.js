app.controller('cartController', function($scope, cartService) {
	// 获得购物车列表
	$scope.findCartList = function() {
		cartService.findCartList().success(function(response) {
			$scope.cartList = response;//购物车列表
			$scope.totalValue = cartService.sum($scope.cartList);//合计数
		})
	}
	// 操作购物车物品和数量
	$scope.addGoodsToCartList = function(itemId, num) {
		cartService.addGoodsToCartList(itemId, num).success(function(response) {
			if (!response.success) {
				// 添加失败
				alert(response.message);
			} else {
				// 添加成功,调用查询购物车列表的方法，异步刷新列表
				$scope.findCartList();
			}
		})
	}
})