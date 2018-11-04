app.controller('itemController', function ($scope, $http) {
    //数量操作
    $scope.addNum = function (x) {
        $scope.num = $scope.num + x;
        if ($scope.num < 1) {
            $scope.num = 1;
        }
    }
    //记录规格选择
    $scope.specificationItems = {};
    $scope.selectSpecification = function (name, value) {
        if (!$scope.isSelected(name, value)) {
            //未选择的项目更改，如果选择了就忽略
            $scope.specificationItems[name] = value;
            searchSku();
        }
    }
    //判断是否选中
    $scope.isSelected = function (name, value) {
        if ($scope.specificationItems[name] == value) {
            return true;
        } else {
            return false;
        }
    }
    //加载默认sku
    $scope.loadSku = function () {
        if (skuList.length > 0) {
            $scope.sku = skuList[0];
            $scope.specificationItems = JSON.parse(JSON.stringify($scope.sku.spec))
        }
    }
    //比较两个map的方法
    matchObject = function (map1, map2) {
        for (var k in map1) {
            if (map1[k] != map2[k]) {
                return false;
            }
        }
        for (var k in map2) {
            if (map2[k] != map1[k]) {
                return false;
            }
        }
        return true;
    }
    //遍历skuList,比较spec的map和specificationItems
    searchSku = function () {
        for (var i = 0; i < skuList.length; i++) {
            if (matchObject(skuList[i].spec, $scope.specificationItems)) {
                //是这个sku
                $scope.sku = skuList[i];
                return;
            }
        }
        $scope.sku = {id: 0, title: '--------', price: 0};//如果没有匹配的
    }
    //添加购物车
    $scope.addToCart = function () {
        $http.get('http://localhost:9107/cart/addGoodsToCartList.do?' +
            'itemId=' + $scope.sku.id + '&num=' + $scope.num, {withCredentials: true}).success(
            function (response) {
                if (response.success) {
                    //添加成功,跳转到购物车页面
                    location.href = 'http://localhost:9107/cart.html';
                } else {
                    alert(response.message);
                }
            })
    }
})	