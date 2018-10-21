//控制层
app
    .controller(
        'goodsDetailController',
        function ($scope, $controller, $location, goodsService,
                  uploadService, itemCatService, typeTemplateService) {

            $controller('baseController', {
                $scope: $scope
            });// 继承

            // 读取列表数据绑定到表单中
            $scope.findAll = function () {
                goodsService.findAll().success(function (response) {
                    $scope.list = response;
                });
            }

            // 分页
            $scope.findPage = function (page, rows) {
                goodsService
                    .findPage(page, rows)
                    .success(
                        function (response) {
                            $scope.list = response.rows;
                            $scope.paginationConf.totalItems = response.total;// 更新总记录数
                        });
            }

            // 查询实体
            $scope.findOne = function () {
                var id = $location.search()['id'];
                if (id) {
                    goodsService
                        .findOne(id)
                        .success(
                            function (response) {
                                $scope.entity = response;
                                editor
                                    .html($scope.entity.goodsDesc.introduction);
                                $scope.entity.goodsDesc.itemImages = JSON
                                    .parse($scope.entity.goodsDesc.itemImages);
                                $scope.entity.goodsDesc.customAttributeItems = JSON
                                    .parse($scope.entity.goodsDesc.customAttributeItems);
                                $scope.entity.goodsDesc.specificationItems = JSON
                                    .parse($scope.entity.goodsDesc.specificationItems);
                                for (var i = 0; i < $scope.entity.itemList.length; i++) {
                                    $scope.entity.itemList[i].spec = JSON
                                        .parse($scope.entity.itemList[i].spec);
                                }
                            });
                }

            }

            // 保存
            $scope.save = function () {
                $scope.entity.goodsDesc.introduction = editor.html();
                var serviceObject;
                if ($scope.entity.goods.id != null) {
                    serviceObject = goodsService.update($scope.entity);
                } else {
                    serviceObject = goodsService.add($scope.entity);
                }
                serviceObject.success(
                    function (response) {
                        if (response.success) {
                            alert(response.message);
                            // 清空数据
                            location.href = "goods.html";
                        } else {
                            alert(response.message);
                        }
                    });
            }

            // 批量删除
            $scope.dele = function () {
                // 获取选中的复选框
                goodsService.dele($scope.selectIds).success(
                    function (response) {
                        if (response.success) {
                            $scope.reloadList();// 刷新列表
                            $scope.selectIds = [];
                        }
                    });
            }

            $scope.searchEntity = {};// 定义搜索对象

            // 搜索
            $scope.search = function (page, rows) {
                goodsService
                    .search(page, rows, $scope.searchEntity)
                    .success(
                        function (response) {
                            $scope.list = response.rows;
                            $scope.paginationConf.totalItems = response.total;// 更新总记录数
                        });
            }

            $scope.uploadFile = function () {
                uploadService.uploadFile().success(function (response) {
                    if (response.success) {
                        $scope.image_entity.url = response.message;
                    } else {
                        alert(response.message);
                    }
                });
            }

            $scope.entity = {
                goods: {},
                goodsDesc: {
                    itemImages: [],
                    specificationItems: []
                }
            };
            // 将当前上传的图片存入图片列表
            $scope.add_image_entity = function () {
                $scope.entity.goodsDesc.itemImages
                    .push($scope.image_entity);
            }

            // 删除图片
            $scope.remove_image_entity = function (index) {
                $scope.entity.goodsDesc.itemImages.splice(index, 1);
            }

            // 查询一级商品分类列表
            $scope.selectItemCatList_1 = function () {
                itemCatService.findByParentId('0').success(
                    function (response) {
                        $scope.itemCatList1 = response;
                        // $scope.entity.goods.typeTemplateId = '';
                    })
            }

            // 查询二级商品分类列表
            $scope
                .$watch(
                    'entity.goods.category1Id',
                    function (newValue, oldValue) {
                        if (newValue) {
                            itemCatService
                                .findByParentId(newValue)
                                .success(
                                    function (response) {
                                        $scope.itemCatList2 = response;
                                        $scope.entity.goods.typeTemplateId = '';
                                    })
                        }
                    });

            // 查询三级商品分类列表
            $scope.$watch('entity.goods.category2Id', function (
                newValue, oldValue) {
                if (newValue) {
                    itemCatService.findByParentId(newValue).success(
                        function (response) {
                            $scope.itemCatList3 = response;
                        })
                }
            });

            // 查询type_template_id
            $scope
                .$watch(
                    'entity.goods.category3Id',
                    function (newValue, oldValue) {
                        if (newValue) {
                            itemCatService
                                .findOne(newValue)
                                .success(
                                    function (response) {
                                        $scope.entity.goods.typeTemplateId = response.typeId;
                                    })
                        }
                    });

            // 根据type_template_id查询品牌字段
            $scope
                .$watch(
                    'entity.goods.typeTemplateId',
                    function (newValue, oldValue) {
                        if (newValue) {
                            typeTemplateService
                                .findOne(newValue)
                                .success(
                                    function (response) {
                                        $scope.template = response;
                                        $scope.template.brandIds = JSON
                                            .parse($scope.template.brandIds);
                                        if ($location
                                            .search()['id'] == null) {
                                            // 如果是没传id就是增加商品
                                            $scope.entity.goodsDesc.customAttributeItems = JSON
                                                .parse($scope.template.customAttributeItems);
                                        }

                                    });
                            // 读取规格
                            typeTemplateService
                                .findSpecList(newValue)
                                .success(
                                    function (response) {
                                        $scope.specList = response;
                                    })
                        } else {
                            $scope.template = {};
                            $scope.specList = [];
                        }
                    })

            // 操作集合的方法
            $scope.updateSpecAttribute = function ($event, name, value) {
                var object = $scope.searchObjectByKey(
                    $scope.entity.goodsDesc.specificationItems,
                    'attributeName', name);
                if (object) {
                    // 不为空
                    // 选中
                    if ($event.target.checked) {
                        object.attributeValue.push(value);
                    } else {
                        object.attributeValue
                            .splice(object.attributeValue
                                .indexOf(value), 1);
                        if (object.attributeValue.length == 0) {
                            $scope.entity.goodsDesc.specificationItems
                                .splice(
                                    $scope.entity.goodsDesc.specificationItems
                                        .indexOf(object), 1);
                        }
                    }
                } else {
                    // 空
                    $scope.entity.goodsDesc.specificationItems.push({
                        "attributeName": name,
                        "attributeValue": [value]
                    });
                }
            }

            // 创建sku列表
            $scope.createItemList = function () {
                $scope.entity.itemList = [{
                    spec: {},
                    price: 0,
                    num: 99999,
                    status: '0',
                    isDefault: '0'
                }];// 列表初始化
                var items = $scope.entity.goodsDesc.specificationItems;

                for (var i = 0; i < items.length; i++) {
                    $scope.entity.itemList = addColumn(
                        $scope.entity.itemList,
                        items[i].attributeName,
                        items[i].attributeValue);
                }
            }

            addColumn = function (list, name, values) {
                var newList = [];
                for (var i = 0; i < list.length; i++) {
                    var oldRow = list[i];
                    for (var j = 0; j < values.length; j++) {
                        var newRow = JSON.parse(JSON.stringify(oldRow));
                        newRow.spec[name] = values[j];
                        newList.push(newRow);
                    }
                }
                return newList;
            }

            $scope.status = ['未审核', '已审核', '审核未通过', '已关闭'];
            // 查询商品分类列表
            $scope.itemCatList = [];
            $scope.findItemCatList = function () {
                itemCatService
                    .findAll()
                    .success(
                        function (response) {
                            for (var i = 0; i < response.length; i++) {
                                $scope.itemCatList[response[i].id] = response[i].name;
                            }
                        });
            }

            $scope.checkAttributeValue = function (specName, optionName) {
                var items = $scope.entity.goodsDesc.specificationItems;
                var object = $scope.searchObjectByKey(items,
                    'attributeName', specName);
                if (object) {
                    if (object['attributeValue'].indexOf(optionName) >= 0) {
                        return true;
                    }
                }
                return false;
            }


            $scope.selectItemCatList_1 = function () {
                itemCatService.findOne('0').success(function (response) {
                    $scope.itemCatList1 = response;
                })
            };
            $scope.$watch('$scope.entity.goods.category1Id',function () {
                itemCatService.findOne($scope.entity.goods.category1Id).success(function (response) {
                    $scope.itemCatList2 = response;
                })
            })
            $scope.$watch('$scope.entity.goods.category2Id',function () {
                itemCatService.findOne($scope.entity.goods.category2Id).success(function (response) {
                    $scope.itemCatList3 = response;
                })
            })
            $scope.$watch('$scope.entity.goods.category3Id',function () {
                typeTemplateService.findOne($scope.entity.goods.category3Id).success(function (response) {
                    $scope.template = response;
                    $template.brandIds = JSON.parse($template.brandIds);
                })
            })
        });
