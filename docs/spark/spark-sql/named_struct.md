# Argmin/argmax

hive按照字段顺序比较struct，例如：{a:1,b:2} < {a:2,b:1}。 

因此，取y列最小的x的方法是： min(named_struct('y', y, 'x', x)).x 