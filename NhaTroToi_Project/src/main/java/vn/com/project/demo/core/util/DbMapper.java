package vn.com.project.demo.core.util;

import javax.persistence.Tuple;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DbMapper {

  private final Map<Class<?>, BiFunction<Tuple, String, ?>> typeConverterMap;

  @Lazy
  @Autowired
  private FormattingConversionService converterRegistry;

  public DbMapper() {
    typeConverterMap = new HashMap<>();
    typeConverterMap.put(String.class, this::getString);
    typeConverterMap.put(Boolean.class, this::getBoolSafe);
    typeConverterMap.put(Byte.class, this::getByteSafe);
    typeConverterMap.put(Short.class, this::getShortSafe);
    typeConverterMap.put(Integer.class, this::getIntegerSafe);
    typeConverterMap.put(Long.class, this::getLongSafe);
    typeConverterMap.put(BigInteger.class, this::getBigIntegerSafe);
    typeConverterMap.put(Float.class, this::getFloatSafe);
    typeConverterMap.put(Double.class, this::getDoubleSafe);
    typeConverterMap.put(BigDecimal.class, this::getBigDecimalSafe);
    typeConverterMap.put(Date.class, (tuple, field) -> tuple.get(field, Date.class));
    typeConverterMap.put(Timestamp.class, (tuple, field) -> tuple.get(field, Timestamp.class));
    typeConverterMap.put(Instant.class, this::getInstantSafe);
  }

  public String getString(Tuple tuple, String field) {
    return tuple.get(field, String.class);
  }

  public Boolean getBoolSafe(Tuple tuple, String field) {
    Number number = tuple.get(field, Number.class);
    return number != null && number.intValue() > 0;
  }

  public Byte getByteSafe(Tuple tuple, String field) {
    Number number = tuple.get(field, Number.class);
    return number == null ? null : number.byteValue();
  }

  public Short getShortSafe(Tuple tuple, String field) {
    Number number = tuple.get(field, Number.class);
    return number == null ? null : number.shortValue();
  }

  public Integer getIntegerSafe(Tuple tuple, String field) {
    Number number = tuple.get(field, Number.class);
    return number == null ? null : number.intValue();
  }

  public Long getLongSafe(Tuple tuple, String field) {
    Number number = tuple.get(field, Number.class);
    return number == null ? null : number.longValue();
  }

  public BigInteger getBigIntegerSafe(Tuple tuple, String field) {
    Number number = tuple.get(field, Number.class);
    return number == null ? null : (BigInteger) number;
  }

  public Float getFloatSafe(Tuple tuple, String field) {
    Number number = tuple.get(field, Number.class);
    return number == null ? null : number.floatValue();
  }

  public Double getDoubleSafe(Tuple tuple, String field) {
    Number number = tuple.get(field, Number.class);
    return number == null ? null : number.doubleValue();
  }

  public BigDecimal getBigDecimalSafe(Tuple tuple, String field) {
    Number number = tuple.get(field, Number.class);
    return number == null ? null : (BigDecimal) number;
  }

  public Instant getInstantSafe(Tuple tuple, String field) {
    Timestamp timestamp = tuple.get(field, Timestamp.class);
    return timestamp == null ? null : timestamp.toInstant();
  }

  private Object getValue(Tuple tuple, String field, Class<?> clazz) {
    BiFunction<Tuple, String, ?> converterFunc = typeConverterMap.get(clazz);
    if (converterFunc == null) {
      Object obj = tuple.get(field);
      return this.converterRegistry.convert(obj, clazz);
    }
    return converterFunc.apply(tuple, field);
  }

  private <R> void getMethodMap(Class<R> clazz, Map<String, Pair<Method, Method>> methodMap) {
    if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
      getMethodMap(clazz.getSuperclass(), methodMap);
    }
    for (Field field : clazz.getDeclaredFields()) {
      DbColumnMapper dbColumnMapper = field.getAnnotation(DbColumnMapper.class);
      if (dbColumnMapper != null) {
        String propPascalCase = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        String getterName = "get" + propPascalCase;
        String setterName = "set" + propPascalCase;
        try {
          Method getter = clazz.getMethod(getterName);
          Method setter = clazz.getMethod(setterName, getter.getReturnType());
          String label = ObjectUtils.isEmpty(dbColumnMapper.value()) ? StringUtil.camelToSnake(propPascalCase)
            : dbColumnMapper.value();
          methodMap.put(label, Pair.of(getter, setter));
        } catch (NoSuchMethodException ignored) {
          log.error("Error get getter setter method of class", ignored);
        }
      }
    }
  }

  @SneakyThrows
  private <R> R castSqlResult(Tuple tuple, Constructor<R> constructor, Map<String, Pair<Method, Method>> methodMap) {
    R instant = constructor.newInstance();
    for (Map.Entry<String, Pair<Method, Method>> getterSetterPair : methodMap.entrySet()) {
      try {
        String label = getterSetterPair.getKey();
        Method getter = getterSetterPair.getValue().getFirst();
        Method setter = getterSetterPair.getValue().getSecond();
        Object value = getValue(tuple, label, getter.getReturnType());
        setter.invoke(instant, value);
      } catch (InvocationTargetException | IllegalAccessException ignored) {
        log.error("Error convert column value to field", ignored);
      }
    }
    return instant;
  }

  @SneakyThrows
  public <R> R castSqlResult(Tuple tuple, Class<R> clazz) {
    Constructor<R> constructor = clazz.getDeclaredConstructor();
    Map<String, Pair<Method, Method>> methodMap = new HashMap<>();
    getMethodMap(clazz, methodMap);
    return castSqlResult(tuple, constructor, methodMap);
  }

  @SneakyThrows
  public <R> List<R> castSqlResult(List<Tuple> tuples, Class<R> clazz) {
    Constructor<R> constructor = clazz.getDeclaredConstructor();
    Map<String, Pair<Method, Method>> methodMap = new HashMap<>();
    getMethodMap(clazz, methodMap);
    return tuples.stream().map(e -> castSqlResult(e, constructor, methodMap)).collect(Collectors.toList());
  }

}
