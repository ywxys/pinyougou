app.service('addressService',function ($http) {
    //获取地址列表
    this.findAddressList = function () {
        return $http.get('address/findListByUserId.do');
    }
    //增加收货地址
    this.add=function (entity) {
        return $http.post('address/add.do', entity);
    }
    //修改收货地址
    this.update=function (entity) {
        return $http.post('address/update.do', entity);
    }
    //查询某个地址
    this.findOne=function (id) {
        return $http.get('address/findOne.do?id=' + id);
    }
    //删除地址
    this.dele=function (id) {
        return $http.get('address/delete.do?ids='+id)
    }
})