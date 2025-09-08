import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;

public class ShamirSecretSharing {

    static BigInteger parseValue(String s, int base) {
        return new BigInteger(s, base);
    }

    // Lagrange interpolation at x=0 using BigInteger
    static BigInteger lagrangeAtZero(List<BigInteger> xs, List<BigInteger> ys, int k) {
        BigInteger secret = BigInteger.ZERO;

        for (int i = 0; i < k; i++) {
            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                num = num.multiply(xs.get(j).negate());
                den = den.multiply(xs.get(i).subtract(xs.get(j)));
            }

            secret = secret.add(ys.get(i).multiply(num).divide(den));
        }

        return secret;
    }

    // Minimal JSON parser
    static Map<String, Map<String, String>> parseJson(String content) {
        Map<String, Map<String, String>> result = new HashMap<>();
        content = content.trim();
        content = content.substring(1, content.length() - 1).trim();
        String[] parts = content.split("},");
        for (String part : parts) {
            part = part.trim();
            if (!part.endsWith("}")) part += "}";
            int colon = part.indexOf(":");
            if (colon == -1) continue;
            String key = part.substring(0, colon).trim().replaceAll("[\"{}]", "");
            String obj = part.substring(colon + 1).trim().replaceAll("[{}\"]", "");
            Map<String, String> kv = new HashMap<>();
            for (String kvp : obj.split(",")) {
                String[] kvs = kvp.split(":");
                if (kvs.length == 2) kv.put(kvs[0].trim(), kvs[1].trim());
            }
            result.put(key, kv);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        String jsonStr = new String(Files.readAllBytes(Paths.get("testcase.json")));
        Map<String, Map<String, String>> obj = parseJson(jsonStr);

        Map<String, String> keys = obj.get("keys");
        int n = Integer.parseInt(keys.get("n"));
        int k = Integer.parseInt(keys.get("k"));

        List<BigInteger> xs = new ArrayList<>();
        List<BigInteger> ys = new ArrayList<>();

        for (String key : obj.keySet()) {
            if (key.equals("keys")) continue;
            BigInteger x = new BigInteger(key);
            int base = Integer.parseInt(obj.get(key).get("base"));
            BigInteger y = parseValue(obj.get(key).get("value"), base);
            xs.add(x);
            ys.add(y);
        }

        // Sort by x
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < xs.size(); i++) order.add(i);
        order.sort(Comparator.comparing(xs::get));

        // Take first k shares
        List<BigInteger> xsK = new ArrayList<>();
        List<BigInteger> ysK = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            xsK.add(xs.get(order.get(i)));
            ysK.add(ys.get(order.get(i)));
        }

        BigInteger secret = lagrangeAtZero(xsK, ysK, k);
        System.out.println("Secret (decimal): " + secret);
        System.out.println("Secret (hex): " + secret.toString(16));
    }
}
