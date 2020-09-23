package org.mapstruct.example.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.control.DeepClone;
import org.mapstruct.example.dto.PersonDTO;
import org.mapstruct.example.po.Person;
import org.mapstruct.factory.Mappers;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-08-19
 */
@Mapper
public interface PersonConverter {
  @Mappings({
      @Mapping(source = "birthday", target = "birth"),
      @Mapping(source = "birthday", target = "birthDateFormat", dateFormat = "yyyy-MM-dd HH:mm:ss"),
      @Mapping(target = "birthExpressionFormat", expression = "java(org.apache.commons.lang3.time.DateFormatUtils.format(person.getBirthday(),\"yyyy-MM-dd HH:mm:ss\"))"),
      @Mapping(source = "user.age", target = "age"),
      @Mapping(target = "email", ignore = true)
  })
  PersonDTO domain2dto(Person person);
  PersonConverter INSTANCE = Mappers.getMapper(PersonConverter.class);

  List<PersonDTO> domain2dto(List<Person> people);
}
