app.controller('cartController', function($scope, cartService, addressService) {
	// 获得购物车列表
	$scope.findCartList = function() {
		cartService.findCartList().success(function(response) {
			$scope.cartList = response;// 购物车列表
			$scope.totalValue = cartService.sum($scope.cartList);// 合计数
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
	// 获取用户地址信息
	$scope.findAddressList = function() {
		addressService.findAddressList().success(function(response) {
			$scope.addressList = response;
			// 寻找默认地址
			for (var i = 0; i < $scope.addressList.length; i++) {
				var address = $scope.addressList[i];
				if (address.isDefault == '1') {
					$scope.address = address;
					break;
				}
			}
		})
	}
	// 选择地址
	$scope.selectAddress = function(address) {
		$scope.address = address;
	}
	// 判断是否是选择的地址
	$scope.isSelectedAddress = function(address) {
		if (address == $scope.address) {
			return true;
		} else {
			return false;
		}
	};
	// 点击设置别名
	$scope.setAlias = function(alias) {
		$scope.entity.alias = alias;
	}
	// 新增地址
	$scope.save = function() {
		var serviceObject = addressService.add($scope.entity);
		if ($scope.entity.id != null) {// 存在id的情况则是修改
			serviceObject = addressService.update($scope.entity);
		}
		serviceObject.success(function(response) {
			if (response.success) {
				// 重新查询地址列表
				$scope.findAddressList();
			} else {
				alert(response.message);
			}
		});
	}
	// 编辑地址时回显数据
	$scope.findOne = function(id) {
		addressService.findOne(id).success(function(response) {// 回显数据
			$scope.entity = response;
		})
	}
	// 删除地址
	$scope.dele = function(id) {
		addressService.dele(id).success(function(response) {
			if (response.success) {
				$scope.findAddressList();
			} else {
				alert(response.message);
			}
		})
	}
	// 支付方式
	$scope.order = {
		paymentType : '1'
	};
	// 选择支付方式
	$scope.selectPayType = function(type) {
		$scope.order.paymentType = type;
	}
	// 提交订单
	$scope.submitOrder = function() {
		$scope.order.receiveAreaName = $scope.address.address;// 地址
		$scope.order.receiverMobile = $scope.address.mobile;// 联系人电话
		$scope.order.receiver = $scope.address.contact;// 联系人
		cartService.submitOrder($scope.order).success(function(response) {
			if (response.success) {// 成功添加订单
				if ($scope.order.paymentType == '1') {// 微信，跳转到支付页面
					location.href = "pay.html";
				} else if ($scope.order.paymentType == '2') {// 货到付款
					location.href = "paysuccess.html";
				}
			} else {// 添加失败
				alert(response.message);
			}
		})
	};
})