package org.mapstruct.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Test;
import org.mapstruct.example.dto.PersonDTO;
import org.mapstruct.example.mapper.PersonConverter;
import org.mapstruct.example.po.Person;
import org.mapstruct.example.po.User;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-08-19
 */
public class PersonConverterTest {
  @Test
  public void test() {
    Person person = new Person(1L,"zhige","zhige.me@gmail.com",new Date(),new User(1));
    PersonDTO personDTO = PersonConverter.INSTANCE.domain2dto(person);
    System.out.println(person);
    System.out.println(personDTO);
    assertNotNull(personDTO);
    assertEquals(personDTO.getId(), person.getId());
    assertEquals(personDTO.getName(), person.getName());
    assertEquals(personDTO.getBirth(), person.getBirthday());
    String format = DateFormatUtils.format(personDTO.getBirth(), "yyyy-MM-dd HH:mm:ss");
    assertEquals(personDTO.getBirthDateFormat(),format);
    assertEquals(personDTO.getBirthExpressionFormat(),format);

    List<Person> people = new ArrayList<>();
    people.add(person);
    List<PersonDTO> personDTOs = PersonConverter.INSTANCE.domain2dto(people);
    assertNotNull(personDTOs);
  }
}
