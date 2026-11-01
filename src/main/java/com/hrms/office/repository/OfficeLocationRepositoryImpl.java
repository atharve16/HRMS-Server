package com.hrms.office.repository;
import com.hrms.office.model.OfficeLocation;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.regex.Pattern;
@Repository
public class OfficeLocationRepositoryImpl implements OfficeLocationRepositoryCustom {
    private static final Map<String,String> SORT=Map.of("locationCode","locationCode","name","name","city","city","status","status","createdAt","createdAt");
    private final MongoTemplate template;
    public OfficeLocationRepositoryImpl(MongoTemplate template){this.template=template;}
    public Page<OfficeLocation> search(OfficeSearch s){
        Criteria c=new Criteria(); if(s.search()!=null&&!s.search().isBlank()){String r=".*"+Pattern.quote(s.search().strip())+".*";c.orOperator(Criteria.where("locationCode").regex(r,"i"),Criteria.where("name").regex(r,"i"),Criteria.where("city").regex(r,"i"));} if(s.status()!=null)c.and("status").is(s.status());
        Sort.Direction d="desc".equalsIgnoreCase(s.sortDirection())?Sort.Direction.DESC:Sort.Direction.ASC; Pageable p=PageRequest.of(s.page(),s.limit(),Sort.by(d,SORT.getOrDefault(s.sortBy(),"createdAt")));
        return new PageImpl<>(template.find(Query.query(c).with(p),OfficeLocation.class),p,template.count(Query.query(c),OfficeLocation.class));
    }
}
