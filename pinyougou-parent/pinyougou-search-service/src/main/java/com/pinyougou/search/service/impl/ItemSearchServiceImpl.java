package com.pinyougou.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.*;

@Service(timeout = 5000)
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private SolrTemplate solrTemplate;

    @Override
    public Map search(Map searchMap) {
        Map result = new HashMap();
                
        //将关键词里的空格缩进
        String keywords = (String) searchMap.get("keywords");
        searchMap.put("keywords", keywords.replace(" ", ""));
        
        //查询列表
        result.putAll(searchList(searchMap));

        //查询商品分类
        List categoryList = searchCategoryList(searchMap);
        result.put("categoryList", categoryList);

        //查询商品分类的品牌和规格缓存
        String categoryName=(String)searchMap.get("category");
        if (!"".equals(categoryName)) {//如果有分类名称
            result.putAll(searchBrandAndSpecList(categoryName));
        } else{
            //没有商品分类条件，按照第一个查询
            if (categoryList.size() > 0) {
                result.putAll(searchBrandAndSpecList((String) categoryList.get(0)));
            }
        }

        return result;
    }

    private Map searchList(Map searchMap) {
        Map map = new HashMap();
        HighlightQuery query = new SimpleHighlightQuery();
        //设置高亮区域
        HighlightOptions highlightOptions = new HighlightOptions().addField("item_title");
        highlightOptions.setSimplePrefix("<em style='color:red'>");//前缀
        highlightOptions.setSimplePostfix("</em>");//后缀
        query.setHighlightOptions(highlightOptions);

        //按照关键字查询
        String keywords = (String) searchMap.get("keywords");
        Criteria criteria=new Criteria("item_keywords") ;
        if("".equals(keywords)) {//没有关键词查所有
        	criteria=criteria.isNotNull();
        }else {//有关键词按关键词查询
        	criteria=criteria.is(searchMap.get("keywords"));
		}
        query.addCriteria(criteria);
        //按分类筛选
        if (!"".equals(searchMap.get("category"))) {
            Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
            query.addFilterQuery(new SimpleFilterQuery(filterCriteria));
        }
        //按品牌筛选
        if (!"".equals(searchMap.get("brand"))) {
            Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
            query.addFilterQuery(new SimpleFilterQuery(filterCriteria));
        }
        //按规格选项筛选
        if(searchMap.get("spec")!=null){
            Map<String,String> specMap= (Map) searchMap.get("spec");
            for(String key:specMap.keySet() ){
                Criteria filterCriteria=new Criteria("item_spec_"+key).is( specMap.get(key) );
                FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
        }
        //按价格区间筛选
        if (!"".equals(searchMap.get("price"))) {
            String[] price = ((String)searchMap.get("price")).split("-");
            if(!price[0].equals("0")){
                //不以0为起点
                Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(price[0]);
                query.addFilterQuery(new SimpleFilterQuery(filterCriteria));
            }
            if (!price[1].equals("*")) {
                //不以*结尾
                Criteria filterCriteria = new Criteria("item_price").lessThanEqual(price[1]);
                query.addFilterQuery(new SimpleFilterQuery(filterCriteria));
            }
        }
        //分页查询
        //页码
        Integer pageNo = (Integer) searchMap.get("pageNo");
        if(pageNo == null) {
            //没传入页码
        	pageNo = 1;
        }
        //每页数量
        Integer pageSize = (Integer)searchMap.get("pageSize");
        if(pageSize==null) {
            pageSize = 20;//默认20条
        }
        //设置分页信息
        query.setOffset((pageNo-1)*pageSize);//开始索引
        query.setRows(pageSize);
        //排序
        String sortValue = (String) searchMap.get("sort");//升序还是降序ASC/DESC
        String sortField = (String) searchMap.get("sortField");//需要排序的字段
        if(!"".equals(sortValue) && !"".equals(sortField)) {
            //排序字段和方式都不为空
        	if(sortValue.equals("ASC")) {
            	//升序
        		Sort sort = new Sort(Sort.Direction.ASC,"item_"+sortField);
        		query.addSort(sort);
        	}
        	if(sortValue.equals("DESC")){
        		//降序
        		Sort sort = new Sort(Sort.Direction.DESC,"item_"+sortField);
        		query.addSort(sort);
			}
        }

        //高亮查询显示结果
        HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
        for (HighlightEntry<TbItem> h : page.getHighlighted()) {
            TbItem item = h.getEntity();
            if (h.getHighlights().size() > 0 && h.getHighlights().get(0).getSnipplets().size() > 0) {
                item.setTitle(h.getHighlights().get(0).getSnipplets().get(0));//设置高亮结果
            }
        }
        map.put("rows", page.getContent());
        map.put("totalPages", page.getTotalPages());//总页数
        map.put("totalCount", page.getTotalElements());//总记录数


        return map;
    }

    /**
     * 查询分类列表
     * @param searchMap
     * @return
     */
    private List searchCategoryList(Map searchMap){
        List<String> list = new ArrayList<>();
        Query query = new SimpleQuery();
        //初始查询条件
        String keywords = (String) searchMap.get("keywords");
        Criteria criteria=new Criteria("item_keywords") ;
        if("".equals(keywords)) {//没有关键词查所有
        	criteria=criteria.isNotNull();
        }else {//有关键词按关键词查询
        	criteria=criteria.is(searchMap.get("keywords"));
		}
        query.addCriteria(criteria);

        //设置分组选项
        GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");
        query.setGroupOptions(groupOptions);

        //得到分组页
        GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
        //根据字段得到分组结果集
        GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
        //得到分组结果入口
        Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
        List<GroupEntry<TbItem>> entryList = groupEntries.getContent();
        for (GroupEntry<TbItem> entry : entryList) {
            list.add(entry.getGroupValue());
        }
        return list;
    }

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 加载品牌和规格列表
     * @param category
     * @return
     */
    private Map searchBrandAndSpecList(String category) {
        Map map = new HashMap();
        Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
        if (typeId != null) {
            List<Map> brandList = (List<Map>) redisTemplate.boundHashOps("brandList").get(typeId);
            map.put("brandList", brandList);
            List<Map> specList = (List<Map>) redisTemplate.boundHashOps("specList").get(typeId);
            map.put("specList", specList);
        }

        return map;
    }
}
