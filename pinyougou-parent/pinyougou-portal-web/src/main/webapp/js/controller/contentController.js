//控制层
app.controller('contentController',function($scope,contentService){
	$scope.contentList=[]
	$scope.findByCategoryId=function(categoryId){
		contentService.findByCategoryId(categoryId).success(function(response){
			$scope.contentList[categoryId]=response;
		});
	}
	$scope.search=function () {
        location.href = "http://192.168.24.58:9104/search.html#?keywords="+$scope.keywords;
    }
})