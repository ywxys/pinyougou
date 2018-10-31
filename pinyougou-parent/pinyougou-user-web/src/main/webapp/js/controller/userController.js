//控制层
app.controller('userController', function ($scope, $controller, userService) {
    $scope.entity={
        'username': '',
        'password': '',
        'phone': ''
    }
    //读取列表数据绑定到表单中  
    $scope.findAll = function () {
        userService.findAll().success(
            function (response) {
                $scope.list = response;
            }
        );
    }

    //分页
    $scope.findPage = function (page, rows) {
        userService.findPage(page, rows).success(
            function (response) {
                $scope.list = response.rows;
                $scope.paginationConf.totalItems = response.total;//更新总记录数
            }
        );
    }

    //查询实体
    $scope.findOne = function (id) {
        userService.findOne(id).success(
            function (response) {
                $scope.entity = response;
            }
        );
    }

    //注册
    $scope.reg = function () {
        if ($scope.entity.username == '' || $scope.entity.password == '' || $scope.checkCode == '') {//检测非空
            alert("表单项不能为空");
            return;
        }
        //检测两次密码是否一致
        if ($scope.entity.password != $scope.password) {
            alert("两次密码输入不一致，请重新输入");
            $scope.entity.password = '';
            $scope.password = '';
            return;
        }

        userService.add($scope.entity,$scope.checkCode).success(
            function (response) {
                alert(response.message);
                if (response.success) {
                    //跳转成功界面
                    location.href = '/register.html';
                }
            }
        );
    }


    //批量删除
    $scope.dele = function () {
        //获取选中的复选框
        userService.dele($scope.selectIds).success(
            function (response) {
                if (response.success) {
                    $scope.reloadList();//刷新列表
                    $scope.selectIds = [];
                }
            }
        );
    }

    $scope.searchEntity = {};//定义搜索对象

    //搜索
    $scope.search = function (page, rows) {
        userService.search(page, rows, $scope.searchEntity).success(
            function (response) {
                $scope.list = response.rows;
                $scope.paginationConf.totalItems = response.total;//更新总记录数
            }
        );
    }

    $scope.sendCode = function () {
        if ($scope.entity.phone=='') {
            alert("请输入手机号");
            return;
        }
        userService.sendCode($scope.entity.phone).success(function (response) {
            alert(response.message);
        });
    };

});
