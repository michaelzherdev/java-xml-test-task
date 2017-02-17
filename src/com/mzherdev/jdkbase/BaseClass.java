package com.mzherdev.jdkbase;

import com.mzherdev.jdkbase.App;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;

/**
 * Created by mikhail on 17.02.17.
 */
public class BaseClass {

    private static final String XML_ONE_FILE_NAME = "1.xml";
    private static final String XML_TWO_FILE_NAME = "2.xml";
    private static final String XSL_SCHEME_FILE_NAME = "/scheme.xsl";

    private Integer N;

    private Connection connection;

    private void createTable() throws SQLException {
        try(Statement statement = connection.createStatement()) {
            DatabaseMetaData databaseMetadata = connection.getMetaData();
            ResultSet resultSet = databaseMetadata.getTables(null, null, "test", null);
            if (resultSet.next()) {
                statement.executeUpdate("DELETE FROM TEST");
            } else{
                statement.executeUpdate("CREATE TABLE TEST (field INTEGER)");
            }
        }
    }

    public BaseClass() throws IOException, SQLException {
        Properties properties = new Properties();
        try (final InputStream stream = App.class.getClassLoader().getResourceAsStream("db.properties")) {
            properties.load(stream);
        }

        String url = properties.getProperty("db.databaseurl");
        String username = properties.getProperty("db.username");
        String password = properties.getProperty("db.password");
        Statement statement = null;
        try {
            connection = DriverManager.getConnection(url, username, password);
            createTable();
        } finally {
            if(statement != null)
                statement.close();
        }
    }

    public Integer getN() {
        return N;
    }

    public void setN(Integer n) {
        this.N = n;
    }

    public Connection getConnection() {
        return connection;
    }


    public void insertData() throws SQLException {
        String sql = "INSERT INTO TEST (field) values(?)";
        try(PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 1; i <= N; i++) {
                ps.setInt(1, i);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public String createXmlOne() throws ParserConfigurationException, TransformerException, IOException, SQLException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db  = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElement("entries");

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT field FROM TEST");
        while (resultSet.next()) {
            Element entry = doc.createElement("entry");
            Element field = doc.createElement("field");
            field.setTextContent(resultSet.getString(1));
            entry.appendChild(field);
            root.appendChild(entry);
        }

        resultSet.close();
        statement.close();

        doc.appendChild(root);
        String res = documentToString(doc);
        writeFile(res, XML_ONE_FILE_NAME);
        return res;
    }

    private String documentToString(Node root) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();

        transformer.transform(new DOMSource(root), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    private  void writeFile(String str, String fileName) throws IOException {
        Path path = Paths.get(fileName);
        try(BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(str);
        }
        System.out.println("First file is saved to: " + path.toAbsolutePath());
    }

    public Path createXmlTwo() throws TransformerException, IOException {
        Path path1 = Paths.get(XML_ONE_FILE_NAME);
        Path path2 = Paths.get(XML_TWO_FILE_NAME);
        InputStream inputXSL = getClass().getResourceAsStream(XSL_SCHEME_FILE_NAME);

        TransformerFactory factory = TransformerFactory.newInstance();
        StreamSource xslStream = new StreamSource(inputXSL);
        Transformer transformer = factory.newTransformer(xslStream);

        StreamSource in = new StreamSource(path1.toFile());
        StreamResult out = new StreamResult(path2.toFile());
        transformer.transform(in, out);

        System.out.println("Second file saved to:" + path2.toAbsolutePath());
        return path2;
    }

    public long parseXml(Path path) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db  = dbf.newDocumentBuilder();
        Document doc = db.parse(path.toFile());

        NodeList list = doc.getElementsByTagName("entry");
        long sum = 0;

        for(int i = 0; i < list.getLength(); i++) {
            String fieldValue = list.item(i).getAttributes().getNamedItem("field").getNodeValue();
            sum += Integer.parseInt(fieldValue);
        }
        return sum;
    }
}
