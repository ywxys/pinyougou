app.controller('searchController', function($scope, searchService) {
	$scope.searchMap = {
		'keywords' : '',
		'category' : '',
		'brand' : '',
		'spec' : {},
		'price' : '',
		'pageNo' : 1,
		'pageSize' : 20,
		'sortField':'',
		'sort':''
	};// 搜索对象

	$scope.search = function() {
		$scope.searchMap.pageNo = parseInt($scope.searchMap.pageNo);// 可能是字符串
		searchService.search($scope.searchMap).success(function(response) {
			$scope.resultMap = response;
			// 构建分页条
			buildPageLabel();
		})
	}

	// 添加搜索项
	$scope.addSearchItem = function(key, value) {
		if (key == 'category' || key == 'brand' || key == 'price') {
			// 如果是点的分类或者是品牌
			$scope.searchMap[key] = value;
		} else {
			// 规格选项的条件
			$scope.searchMap.spec[key] = value;
		}
		$scope.searchMap.pageNo = 1;
		$scope.search();// 执行搜索
	}

	// 删除搜索项
	$scope.removeSearchItem = function(key) {
		if (key == 'category' || key == 'brand' || key == 'price') {
			// 如果是点的分类或者是品牌
			$scope.searchMap[key] = "";
		} else {
			// 规格选项的条件
			delete $scope.searchMap.spec[key];
		}
		$scope.search();// 执行搜索
	};

	// 构建分页条
	buildPageLabel = function() {
		$scope.pageLabel = [];
		$scope.firstDot = false;
		$scope.lastDot = false;
		var maxPage = $scope.resultMap.totalPages;
		var firstPage = 1;
		var lastPage = maxPage;
		var currentPage = $scope.searchMap.pageNo;
		if (maxPage > 5) {// 总页数大于5
			if (currentPage <= 3) {
				lastPage = 5;
				$scope.lastDot = true;// 后面有点
			} else if (currentPage >= maxPage - 2) {
				firstPage = maxPage - 4;
				lastPage = maxPage;
				$scope.firstDot = true;// 前面有点
			} else {
				firstPage = currentPage - 2;
				lastPage = currentPage + 2;
				// 前后都有点
				$scope.firstDot = true;
				$scope.lastDot = true;
			}
		}
		for (var i = firstPage; i <= lastPage; i++) {
			$scope.pageLabel.push(i);
		}
	}

	// 根据页码查询
	$scope.queryByPage = function(pageNo) {
		// 页码是否合理
		if (pageNo <= 0 || pageNo > $scope.resultMap.totalPages) {
			return;
		}
		$scope.searchMap.pageNo = pageNo;
		$scope.search();
	}
	
	//排序搜索
	$scope.sortSearch=function(sortField,sort){
		$scope.searchMap.sortField = sortField;
		$scope.searchMap.sort=sort;
		$scope.search();
	}
})