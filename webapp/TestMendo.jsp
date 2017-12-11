<%@page errorPage="error.jsp"
%><%@page contentType="text/html;charset=UTF-8" pageEncoding="ISO-8859-1"
%><%@page import="java.io.ByteArrayInputStream"
%><%@page import="java.io.File"
%><%@page import="java.io.FileInputStream"
%><%@page import="java.io.InputStream"
%><%@page import="java.io.PrintWriter"
%><%@page import="java.io.ByteArrayOutputStream"
%><%@page import="java.util.Enumeration"
%><%@page import="java.util.Properties"
%><%@page import="java.util.Vector"
%><%@page import="org.w3c.dom.Document"
%><%@page import="org.w3c.dom.Element"
%><%@page import="org.w3c.dom.Node"
%><%@page import="org.w3c.dom.NodeList"
%><%@page import="com.purplehillsbooks.xml.Mel"
%><%@page import="com.purplehillsbooks.xmltest.Test1"
%><%@page import="com.purplehillsbooks.testframe.TestRecorderText"
%>

<h1>Mendocino Testing</h1>
<pre>
<%
    Mel me = Mel.readInputStream(getData1Stream());

    ServletContext sc = session.getServletContext();
    String configPath = sc.getRealPath("/WEB-INF");

    TestRecorderText tr = new TestRecorderText(out, true, new String[0], configPath, new Properties());
    Test1 t1 = new Test1();
    t1.runTests(tr);

%></pre><hr><pre><%

    me.reformatXML();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    me.writeToOutputStream(baos);
    ar.writeHtml(baos.toString("UTF-8"));

%>
</pre>
<h1>all done</h1>

<%!

    public boolean testNotNull(Object value, String description)
        throws Exception
    {
        if (value!=null)
        {
            return true;
        }

        //throw exception for now .. something better later
        throw new Exception("Test failure, got an unexpected null for the situation: "
            +description);
    }

    public boolean testNull(Object value, String description)
        throws Exception
    {
        if (value==null)
        {
            return true;
        }

        //throw exception for now .. something better later
        throw new Exception("Test failure, expected a null but did not get one for the situation: "
            +description);
    }

    public boolean testVal(String value, String expectedValue, String description)
        throws Exception
    {
        if (value!=null && value.equals(expectedValue))
        {
            return true;
        }

        //throw exception for now .. something better later
        throw new Exception("Test failure, expected the value '"+
            expectedValue+"' but instead got the value '"+
            value+"' for the situation: "+description);
    }

    public boolean testScalar(Mel me, String eName, String expectedValue, String description)
        throws Exception
    {
        String value = me.getScalar(eName);
        if (value!=null && value.equals(expectedValue))
        {
            return true;
        }

        //throw exception for now .. something better later
        throw new Exception("Test failure, expected the value '"+
            expectedValue+"' but instead got the value '"+
            value+"' for the scaler value '"+eName+"' for  "+description);
    }

    private static void documentStructure(Writer out, Mel me, String indent)
        throws Exception
    {
        out.write("\n"+indent+"Name: "+me.getName());

        Vector chilluns = me.getChildren("userprofile");
        Enumeration e = chilluns.elements();
        while (e.hasMoreElements())
        {
            documentStructure2(out, (Mel)e.nextElement(), indent+"+--");
        }
    }
    private static void documentStructure2(Writer out, Mel me, String indent)
        throws Exception
    {
        out.write("\n"+indent+"Name: "+me.getName());

        Vector chilluns = me.getChildren("idrec");
        Enumeration e = chilluns.elements();
        while (e.hasMoreElements())
        {
            documentStructure3(out, (Mel)e.nextElement(), indent+"+--");
        }
    }
    private static void documentStructure3(Writer out, Mel me, String indent)
        throws Exception
    {
        out.write("\n"+indent+"Name: "+me.getName());
    }


    private static InputStream getData1Stream()
        throws Exception
    {
        StringBuffer sb = new StringBuffer();
        sb.append(  "<userprofiles>");
        sb.append("\n  <userprofile id=\"MOBQNTGYF\">");
        sb.append("\n    <homepage>http://web.com/processleaves/p/main/public.htm</homepage>");
        sb.append("\n    <lastlogin>1244434616875</lastlogin>");
        sb.append("\n    <lastupdated>1241018683687</lastupdated>");
        sb.append("\n    <username>AAA</username>");
        sb.append("\n  </userprofile>");
        sb.append("\n  <userprofile id=\"YVXIXTGYF\">");
        sb.append("\n    <idrec loginid=\"aaa@gmail.com\"/>");
        sb.append("\n    <username>BBB</username>");
        sb.append("\n  </userprofile>");
        sb.append("\n  <userprofile id=\"HMSBPXKYF\">");
        sb.append("\n    <idrec loginid=\"jjj@a.com\"/>");
        sb.append("\n    <idrec confirmed=\"true\" loginid=\"ddd@a.com\"/>");
        sb.append("\n    <lastlogin>1244512041541</lastlogin>");
        sb.append("\n    <username>CCC</username>");
        sb.append("\n  </userprofile>");
        sb.append("\n</userprofiles>");
        String sbx = sb.toString();
        byte[] buf = sbx.getBytes("UTF-8");
        return new ByteArrayInputStream(buf);
    }


%>