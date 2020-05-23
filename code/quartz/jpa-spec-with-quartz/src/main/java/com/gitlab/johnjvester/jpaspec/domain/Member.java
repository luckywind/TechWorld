package com.gitlab.johnjvester.jpaspec.domain;

import lombok.Data;

import javax.persistence.*;
import java.util.Set;

@Data
@Entity
public class Member {
    @Id
    @GeneratedValue
    private long id;
    private String firstName;
    private String lastName;
    private String zipCode;
    private String interests;
    private boolean active;

    @JoinTable(name = "Member_Class_Cross_Ref",
            joinColumns = @JoinColumn(
                    name = "member_id",
                    referencedColumnName = "id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "class_id",
                    referencedColumnName = "id"
            ))
    @ManyToMany
    private Set<MemberClass> memberClasses;

    public Member() { }
}
