app.controller('seckillGoodsController', function ($scope, $location, seckillGoodsService) {
    $scope.findList = function () {
        seckillGoodsService.findList().success(function (response) {
            $scope.seckillGoodsList = response;
        })
    }
    $scope.findOne = function () {
        //从请求路径上获取id
        var id = $location.search("id")[0];
        seckillGoodsService.findOne(id).success(function (response) {
            $scope.entity = response;
            var second = Math.floor((new Date($scope.entity.endTime).getTime() - new Date().getTime()) / 1000);//总秒数
            var time = $interval(function () {
                if (second > 0) {
                    second -= 1;
                    $scope.timeString = convertTimeString(second);
                } else {
                    $interval.cancel(time);
                    alert("秒杀已结束")
                }
            }, 1000)
        })
    }
    //将秒数改为字符串
    var convertTimeString = function (second) {
        var days = Math.floor(second / 60 / 60 / 24);//天
        var hours = Math.floor(second / 60 / 60 - days * 24);//小时
        var minutes = Math.floor(second / 60 - days * 24 * 60 - hours * 60);//分钟
        var seconds = Math.floor(second - days * 24 * 60 * 60 - hours * 60 * 60 - minutes * 60);//秒
        var timeString = "";
        if (days > 0) {
            timeString = days + "天";
        }
        return timeString + hours + ":" + minutes + ":" + seconds;
    };

    $scope.submitOrder=function () {
        seckillGoodsService.submitOrder($scope.entity.id).success(function (response) {
            if (response.success) {
                //下单成功
                alert("下单成功，请在1分钟内完成支付");
                location.href="pay.html";
            }else {
                alert(response.message);
            }
        })
    }
})