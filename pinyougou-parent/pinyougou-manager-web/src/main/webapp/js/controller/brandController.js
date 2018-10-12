//控制层
app.controller("brandController",function($scope,$controller,brandService){
	//继承基本控制器
	$controller('baseController',{$scope:$scope})
	
	$scope.findAll=function(){
		brandService.findAll().success(function(response){
			$scope.list=response;
		});
	}
	
	//分页
	$scope.findPage=function(page,size){
		brandService.findPage(page,size).success(
			function(response){
				$scope.list=response.rows;//显示当前页的数据
				$scope.paginationConf.totalItems=response.total;//更新总记录数
		});
	}
	
	//新增
	$scope.save=function(){
		var methodName='add';//方法名
		if($scope.entity.id!=null){
			methodName='update';
		}
		brand.save(methodName,$scope.entity).success(
			function(response){
				if(response.success){
					$scope.reloadList();//刷新
				}else{
					alert(response.message);
				}
			});
	}
	
	//查找一个
	$scope.findOne=function(id){
		brandService.findOne(id).success(function(response){
			$scope.entity=response;
		});
	}
	
	//删除
	$scope.dele=function(){
		if(confirm("确认删除这"+$scope.selectIds.length+"项吗?")){
            brandService.dele($scope.selectIds).success(function(response){
                if(response.success){
                    $scope.reloadList();
                }else{
                    alert(response.message);
                }
            });
		}

	};
	
	$scope.searchEntity={};
	//条件查询
	$scope.search=function(page,size){
		brandService.search(page,size,$scope.searchEntity).success(
				function(response){
					$scope.list=response.rows;//显示当前页的数据
					$scope.paginationConf.totalItems=response.total;//更新总记录数
			});
	}
});