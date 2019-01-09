package utils;
/**
 * 使用jva built-in arraycopy方法进行对byte数组的合并
 * @author Yibing Zhang
 *
 */
public class ArrayUtils
{
    /**
             * 合并byte array
     * @param firstArray 第一串byte
     * @param secondArray 第二串byte
     * @return 合并后的byte组
     */
    public static byte[] concat(byte[] firstArray, byte[] secondArray) {
        if (firstArray == null || secondArray == null) {
            return null;
        }
        byte[] bytes = new byte[firstArray.length + secondArray.length];
        System.arraycopy(firstArray, 0, bytes, 0, firstArray.length);
        System.arraycopy(secondArray, 0, bytes, firstArray.length, secondArray.length);
        return bytes;
    }

}
