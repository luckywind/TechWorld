mapper接口

```java
  List<DwSysBusiness> findByMap(@Param("map") Map map);
```

xml文件

```xml
  <select id="findByMap" parameterType="java.util.Map" resultMap="BaseResultMap"
    resultType="com..data.datapi.domain.DwSysBusiness">
    select <include refid="Base_Column_List"></include> from dw_sys_business t where
    <choose>
      <when test="map.keys.size > 0">
        <foreach collection="map.keys" item="key"
          separator="AND">
          ${key} = #{map[${key}]}
        </foreach>
      </when>
      <otherwise>
        1=1
      </otherwise>
    </choose>
  </select>
```

