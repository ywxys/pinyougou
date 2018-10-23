app.controller('searchController', function($scope, searchService) {
	$scope.searchMap={'keywords':'','category':'','brand':'','spec':{},price:''};//搜索对象
	$scope.search = function() {
		searchService.search($scope.searchMap).success(function(response) {
			$scope.resultMap=response;
		})
	}

	//添加搜索项
	$scope.addSearchItem=function (key, value) {
        if (key == 'category' || key == 'brand'||key=='price') {
            //如果是点的分类或者是品牌
            $scope.searchMap[key] = value;
        } else {
        	//规格选项的条件
            $scope.searchMap.spec[key] = value;
        }
        $scope.search();//执行搜索
    }

    //删除搜索项
    $scope.removeSearchItem = function (key) {
        if (key == 'category' || key == 'brand'||key=='price') {
            //如果是点的分类或者是品牌
            $scope.searchMap[key] = "";
        } else {
            //规格选项的条件
            delete $scope.searchMap.spec[key];
        }
        $scope.search();//执行搜索
    };
})