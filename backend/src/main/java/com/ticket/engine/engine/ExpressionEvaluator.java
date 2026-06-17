package com.ticket.engine.engine;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExpressionEvaluator {

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    public boolean evaluate(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return true;
        }

        try {
            Expression parsedExpression = expressionParser.parseExpression(expression);
            EvaluationContext evaluationContext = createEvaluationContext(context);
            Boolean result = parsedExpression.getValue(evaluationContext, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (ParseException e) {
            throw new IllegalArgumentException("表达式解析失败: " + expression, e);
        } catch (Exception e) {
            throw new IllegalArgumentException("表达式求值失败: " + expression, e);
        }
    }

    public boolean evaluateSafe(String expression, Map<String, Object> context) {
        try {
            return evaluate(expression, context);
        } catch (Exception e) {
            return false;
        }
    }

    private EvaluationContext createEvaluationContext(Map<String, Object> context) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        if (context != null) {
            context.forEach(evaluationContext::setVariable);
        }
        return evaluationContext;
    }
}
