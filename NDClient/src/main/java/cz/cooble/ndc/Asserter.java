package cz.cooble.ndc;

public class Asserter {
    public static void ASSERT(boolean b,String message){
        if(b)
            return;
        throw new RuntimeException(message);
    }
    public static void ND_ERROR(String message,Object... vars){

        String[] o = new String[vars.length];
        for (int i = 0; i < vars.length; i++)
            o[i]=vars[i]!=null?vars[i].toString():"null";

        System.err.println(String.format(message.replaceAll("\\{\\}","%s"),(String[])o));
    }
    public static void ND_TRACE(String message,Object... vars){

        String[] o = new String[vars.length];
        for (int i = 0; i < vars.length; i++)
            o[i]=vars[i]!=null?vars[i].toString():"null";

        System.out.println(String.format(message.replaceAll("\\{\\}","%s"),(String[])o));
    }
    public static void ND_WARN(String message,Object... vars){

        String[] o = new String[vars.length];
        for (int i = 0; i < vars.length; i++)
            o[i]=vars[i]!=null?vars[i].toString():"null";

        System.out.println(String.format(message.replaceAll("\\{\\}","%s"),(String[])o));
    }
    public static String ND_RESLOC(String path) {
        if(path.startsWith("res/"))
            return path;
        return "res/"+path;
    }

}
