package com.hrms.shift.repository;
import com.hrms.shift.model.Shift;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.regex.Pattern;
@Repository
public class ShiftRepositoryImpl implements ShiftRepositoryCustom {
    private static final Map<String,String> SORT=Map.of("shiftCode","shiftCode","name","name","status","status","startTime","startTime","createdAt","createdAt");
    private final MongoTemplate template; public ShiftRepositoryImpl(MongoTemplate template){this.template=template;}
    public Page<Shift> search(ShiftSearch s){Criteria c=new Criteria();if(s.search()!=null&&!s.search().isBlank()){String r=".*"+Pattern.quote(s.search().strip())+".*";c.orOperator(Criteria.where("shiftCode").regex(r,"i"),Criteria.where("name").regex(r,"i"),Criteria.where("description").regex(r,"i"));}if(s.status()!=null)c.and("status").is(s.status());Sort.Direction d="desc".equalsIgnoreCase(s.sortDirection())?Sort.Direction.DESC:Sort.Direction.ASC;Pageable p=PageRequest.of(s.page(),s.limit(),Sort.by(d,SORT.getOrDefault(s.sortBy(),"createdAt")));return new PageImpl<>(template.find(Query.query(c).with(p),Shift.class),p,template.count(Query.query(c),Shift.class));}
}
