package com.teamEWSN.gitdeun.common.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Component;

@Component
public class MySqlFunctionContributor implements FunctionContributor {
  @Override
  public void contributeFunctions(FunctionContributions fc) {
    BasicType<Double> doubleType =
        fc.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.DOUBLE);

    // match_against_boolean(title, content, query) -> MATCH(title, content) AGAINST (? IN BOOLEAN MODE)
    fc.getFunctionRegistry().registerPattern(
        "match_against_boolean",
        "match(?1, ?2) against (?3 in boolean mode)",
        doubleType
    );
  }
}