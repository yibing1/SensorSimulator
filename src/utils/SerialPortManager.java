package utils;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;
/**
 * SerialPort Manager 基于RXTXcomm的串口管理程序，用于发现串口，打开串口， 关闭串口，
   *   读取串口数据，发送数据至串口，更改串口波特率，添加事件监听器的功能。
   *  借鉴：https://blog.csdn.net/yy339452689/article/details/77772421
 * @author Yibing Zhang
 *
 */
public class SerialPortManager
{

    /**
               * 查找所有可用端口
     * 
     * @return 可用端口名称列表
     */
    @SuppressWarnings("unchecked")
    public static final ArrayList<String> findPort()
    {
        // 获得当前所有可用串口
        Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
        ArrayList<String> portNameList = new ArrayList<String>();
        // 将可用串口名添加到List并返回该List
        while (portList.hasMoreElements())
        {
            String portName = portList.nextElement().getName();
            portNameList.add(portName);
        }
        return portNameList;
    }


    /**
               *    打开串口
     * @param portName 串口名称
     * @param baudrate 波特率
     * @return 串口对象
     * @throws SerialPortParameterFailure 设置串口参数失败
     * @throws UnsupportedCommOperationException
     * @throws NoSuchPortException
     * @throws PortInUseException
     */
    public final static SerialPort openPort(String portName, int baudrate)
    {
        try
        {
            // 通过端口名识别端口
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
            // 打开端口，设置端口名与timeout（打开操作的超时时间）
            CommPort commPort = portIdentifier.open(portName, 2000);
            // 判断是不是串口
            if (commPort instanceof SerialPort)
            {
                SerialPort serialPort = (SerialPort)commPort;
                // openPortList.add(serialPort);
                try
                {
                    // 设置串口的波特率等参数
                    serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);

                }
                catch (UnsupportedCommOperationException e)
                {
                    System.err.println();
                }
                return serialPort;
            }
            else
            {
                // 不是串口
                System.err.println("Not a instance of port");
            }
        }
        catch (NoSuchPortException e1)
        {
            System.err.println("No such port exists");
        }
        catch (PortInUseException e2)
        {
            System.err.println("This port is in use");
        }
        return null;
    }

    /**
             * 更改串口
    * @param baudRate 波特率
    * @param port 串口
    * @throws UnsupportedCommOperationException
    */
    public static void changeBuadeRate(int baudRate, SerialPort port) throws UnsupportedCommOperationException
    {
        port.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
    }


    /**
     * 关闭串口
     * 
     * @param serialport
     *            待关闭的串口对象
     */
    public static void closePort(SerialPort port)
    {
        if (port != null)
        {
            System.out.println("closing port " + port.getName());
            port.close();
        }
    }


    /**
               * 向串口发送数据
     * 
     * @param serialPort 串口对象
     * @param order  待发送数据
     * @throws SendDataToSerialPortFailure
     * @throws SerialPortOutputStreamCloseFailure
     */
    public static void sendToPort(SerialPort serialPort, byte[] order)
    {
        OutputStream out = null;
        try
        {
            out = serialPort.getOutputStream();
            out.write(order);
            out.flush();
        }
        catch (IOException e)
        {
            System.err.println("Fail to send the data");
            closePort(serialPort);
        }
        finally
        {
            try
            {
                if (out != null)
                {
                    out.close();
                    out = null;
                }
            }
            catch (IOException e)
            {
                System.err.println("Output Stream has been closed");
            }
        }
    }


    /**
     * 从串口读取数据
     * 
     * @param serialPort当前已建立连接的SerialPort对象
     * @return 读取到的数据
     * @throws ReadDataFromSerialPortFailure 从串口读取数据时出错
     * @throws SerialPortInputStreamCloseFailure 关闭串口对象输入流出错
     */
    public static byte[] readFromPort(SerialPort serialPort)
    {
        InputStream in = null;
        byte[] bytes = null;
        try
        {
            in = serialPort.getInputStream();
            // 获取buffer里的数据长度
            int bufflenth = in.available();
            while (bufflenth != 0)
            {
                // 初始化byte数组为buffer中数据的长度
                bytes = new byte[bufflenth];
                in.read(bytes);
                bufflenth = in.available();
            }
        }
        catch (IOException e)
        {
            System.err.println("Fail to read data from port");
        }
        finally
        {
            try
            {
                if (in != null)
                {
                    in.close();
                    in = null;
                }
            }
            catch (IOException e)
            {
                System.err.println("Input Stream has been closed");
            }
        }
        return bytes;
    }


    /**
               * 添加监听器
     * 
     * @param port 串口对象
     * @param listener 串口监听器
     * @throws TooManyListeners 监听类对象过多
     */
    public static void addListener(SerialPort port, DataAvailableListener listener)
    {
        try
        {
            // 给串口添加监听器
            port.addEventListener(new SerialPortListener(listener));
            // 设置当有数据到达时唤醒监听接收线程
            port.notifyOnDataAvailable(true);
            // 设置当通信中断时唤醒中断线程
            port.notifyOnBreakInterrupt(true);
        }
        catch (TooManyListenersException e)
        {
            System.err.println("Already have a listener");
        }
    }

    /**
     * SerialPortListener 自定义监听器,implements SerialPortEventListener interface
                * 借鉴：https://docs.oracle.com/cd/E17802_01/products/products/javacomm/reference/api/javax/comm/SerialPortEventListener.html
     * @author Yibing Zhang
     *
     */
    public static class SerialPortListener implements SerialPortEventListener
    {
        private DataAvailableListener mDataAvailableListener;
        
        public SerialPortListener(DataAvailableListener mDataAvailableListener)
        {
            this.mDataAvailableListener = mDataAvailableListener;
        }

        public void serialEvent(SerialPortEvent serialPortEvent)
        {
            // https://docs.oracle.com/cd/E17802_01/products/products/javacomm/reference/api/javax/comm/SerialPortEvent.html
            switch (serialPortEvent.getEventType())
            {
                case SerialPortEvent.DATA_AVAILABLE: // 1.data available at the
                                                     // serial port
                    if (mDataAvailableListener != null)
                    {
                        mDataAvailableListener.dataAvailable();
                    }
                    break;

                case SerialPortEvent.OUTPUT_BUFFER_EMPTY: // 2.Output buffer is
                                                          // empty.
                    break;

                case SerialPortEvent.CTS: // 3.Clear to send.
                    break;

                case SerialPortEvent.DSR: // 4.Data set ready.
                    break;

                case SerialPortEvent.RI: // 5.Ring indicator.
                    break;

                case SerialPortEvent.CD: // 6.Carrier detect.
                    break;

                case SerialPortEvent.OE: // 7.Overrun error.
                    break;

                case SerialPortEvent.PE: // 8.Parity error.
                    break;

                case SerialPortEvent.FE: // 9.Framing error.
                    break;

                case SerialPortEvent.BI: // 10. Break interrupt.
                    System.err.println("Break interrupt");
                    break;
                default:
                    break;
            }
        }
       
    }


    /**
     * interface for the data available listener
     */
    public interface DataAvailableListener
    {
        void dataAvailable();
    }
}
