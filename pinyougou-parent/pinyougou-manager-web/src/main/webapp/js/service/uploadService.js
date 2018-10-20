app.service("uploadService",function($http){
	//上传文件
	this.uploadFile=function(){
		var formdata=new FormData();
		formdata.append('file',upload.files[0]);//file:文件上传框的name
		
		return $http({
			url:'../upload.do',
			method:'post',
			data:formdata,
			headers:{'Content-Type':undefined},
			transformRequest: angular.identity
		});
	}
})