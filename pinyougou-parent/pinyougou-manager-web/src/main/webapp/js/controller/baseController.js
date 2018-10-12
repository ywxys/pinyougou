app.controller("baseController", function($scope) {
	// 分页控件配置
	$scope.paginationConf = {
		currentPage : 1, // 当前页
		totalItems : 10, // 总记录数
		itemsPerPage : 10,// 每页记录数
		perPageOptions : [ 10, 20, 30, 40, 50 ],// 分页选项
		onChange : function() {// 页码变更后自动触发的方法
			$scope.reloadList();
		}
	}

	$scope.reloadList = function() {
		$scope.search($scope.paginationConf.currentPage,
				$scope.paginationConf.itemsPerPage);// 重新加载
	}

	$scope.selectIds = [];// 用户勾选的id集合
	// 用户勾选复选框
	$scope.updateSelection = function($event, id) {
		if ($event.target.checked) {
			$scope.selectIds.push(id);// 向集合添加元素
		} else {
			var index = $scope.selectIds.indexOf(id);// 查找位置
			$scope.selectIds.splice(index, 1);// 参数一：移除的位置，参数二：移除的个数
		}
	}

	// 优化列表显示
	$scope.jsonToString = function(jsonString, key) {
		var json = JSON.parse(jsonString);
		var value = "";
		for (var i = 0; i < json.length; i++) {
			if (i > 0) {
				value += ","
			}
			value += json[i][key];

		}
		return value;
	}

})