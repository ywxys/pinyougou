app('loginService',function($http){
	this.showName=function(){
		return $http.get('../login/name.do')
	}
})