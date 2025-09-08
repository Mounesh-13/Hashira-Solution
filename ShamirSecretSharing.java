import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;

public class ShamirSecretSharing {

    // Convert string in given base to BigInteger
    static BigInteger parseValue(String s, int base) {
        return new BigInteger(s, base);
    }

    // Lagrange interpolation at x=0
    static BigInteger lagrangeAtZero(List<Integer> xs, List<BigInteger> ys, int k) {
        BigInteger resultNum = BigInteger.ZERO;
        BigInteger resultDen = BigInteger.ONE;

        for (int i = 0; i < k; i++) {
            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                num = num.multiply(BigInteger.valueOf(-xs.get(j)));
                den = den.multiply(BigInteger.valueOf(xs.get(i) - xs.get(j)));
            }

            // term = yi * num/den
            BigInteger termNum = ys.get(i).multiply(num);
            BigInteger termDen = den;

            // add to total: resultNum/resultDen += termNum/termDen
            resultNum = resultNum.multiply(termDen).add(termNum.multiply(resultDen));
            resultDen = resultDen.multiply(termDen);

            // simplify fraction
            BigInteger gcd = resultNum.gcd(resultDen);
            if (!gcd.equals(BigInteger.ONE)) {
                resultNum = resultNum.divide(gcd);
                resultDen = resultDen.divide(gcd);
            }
        }

        // Handle denominator cases
        if (!resultDen.equals(BigInteger.ONE)) {
            if (resultDen.equals(BigInteger.valueOf(-1))) {
                resultNum = resultNum.negate();
                resultDen = BigInteger.ONE;
            } else {
                throw new RuntimeException("Result not integer! Got fraction " + resultNum + "/" + resultDen);
            }
        }

        return resultNum;
    }

    // Very simple JSON parser (works for your testcase.json structure)
    static Map<String, Map<String, String>> parseJson(String content) {
        Map<String, Map<String, String>> result = new HashMap<>();

        content = content.trim();
        content = content.substring(1, content.length() - 1).trim(); // remove { }

        // split top-level objects by "},"
        String[] parts = content.split("},");
        for (String part : parts) {
            part = part.trim();
            if (!part.endsWith("}")) part = part + "}";
            int colon = part.indexOf(":");
            if (colon == -1) continue;

            String key = part.substring(0, colon).trim().replaceAll("[\"{}]", "");
            String obj = part.substring(colon + 1).trim();
            obj = obj.replaceAll("[{}\"]", "");

            Map<String, String> kv = new HashMap<>();
            for (String kvp : obj.split(",")) {
                String[] kvs = kvp.split(":");
                if (kvs.length == 2) {
                    kv.put(kvs[0].trim(), kvs[1].trim());
                }
            }
            result.put(key, kv);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        // Read JSON file
        String jsonStr = new String(Files.readAllBytes(Paths.get("testcase.json")));

        Map<String, Map<String, String>> obj = parseJson(jsonStr);

        // Read keys
        Map<String, String> keys = obj.get("keys");
        int n = Integer.parseInt(keys.get("n"));
        int k = Integer.parseInt(keys.get("k"));

        // Extract shares
        List<Integer> xs = new ArrayList<>();
        List<BigInteger> ys = new ArrayList<>();

        for (String key : obj.keySet()) {
            if (key.equals("keys")) continue;
            int x = Integer.parseInt(key);
            int base = Integer.parseInt(obj.get(key).get("base"));
            String value = obj.get(key).get("value");
            BigInteger y = parseValue(value, base);

            xs.add(x);
            ys.add(y);
        }

        // sort by x
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < xs.size(); i++) order.add(i);
        order.sort(Comparator.comparingInt(xs::get));

        // pick first k shares
        List<Integer> xsK = new ArrayList<>();
        List<BigInteger> ysK = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            xsK.add(xs.get(order.get(i)));
            ysK.add(ys.get(order.get(i)));
        }

        // compute secret
        BigInteger secret = lagrangeAtZero(xsK, ysK, k);
        System.out.println("Secret (decimal): " + secret);
        System.out.println("Secret (hex): " + secret.toString(16));
    }
}
