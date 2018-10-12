package io.mine.protocol.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public class Util {

    protected Util() {

    }

    public static String multiply(String data, int times) {
        if (times <= 1)
            return data;

        return data + multiply(data, times - 1);
    }

    public static List<Object[]> generateParametersCombination(List<List<Object>> parametersValues) {
        List<List<Object>> combinations = doGenerateParametersCombination(parametersValues);
        List<Object[]> results = new ArrayList<>();
        combinations.forEach(e -> results.add(e.toArray()));
        return results;
    }

    private static List<List<Object>> doGenerateParametersCombination(List<List<Object>> parametersValues) {
        List<List<Object>> result = new ArrayList<>();
        if (parametersValues == null || parametersValues.size() == 0)
            return result;

        List<Object> parameter0Values = parametersValues.get(0);
        if (parametersValues.size() == 1) {
            for (int i = 0; i < parameter0Values.size(); i++) {
                List<Object> combination = Arrays.asList(parameter0Values.get(i));
                result.add(combination);
            }

            return result;
        }

        List<List<Object>> subCombinations = doGenerateParametersCombination(
                parametersValues.subList(1, parametersValues.size()));
        for (int i = 0; i < parameter0Values.size(); i++) {
            for (int j = 0; j < subCombinations.size(); j++) {
                List<Object> combination = new ArrayList<>();
                result.add(combination);
                combination.add(parameter0Values.get(i));
                List<Object> subCombination = subCombinations.get(j);
                combination.addAll(subCombination);
            }
        }

        return result;
    }

}
