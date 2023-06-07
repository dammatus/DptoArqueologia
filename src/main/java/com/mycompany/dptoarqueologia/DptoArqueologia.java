package com.mycompany.dptoarqueologia;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.util.AnimatedIcon;
import com.toedter.calendar.JDateChooser;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.UIManager;

public class DptoArqueologia extends javax.swing.JFrame {

    // Valores para la conexión a la base de datos (nombre, URL, Usuario y Contraseña)
    private static final String DB_NAME = "lab-bd";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/";
    private static final String DB_USER = "postgres";
    private static final String DB_PWD = "admin";
    private static final String CREADORES = """
                                            Bienvenido/a: Este Programa fue creado por:
                                                        Matus Damian
                                                        Matrero Bruno
                                            Esperamos que lo disfruten... No lo rompan
                                            """;
    private static final String MENSAJE_BIENVENIDA = """
                                                     Bienvenido/a
                                                     Este es la primera vez que es ejecutado este programa, por lo tanto
                                                     vamos a realizar unas opecaciones iniciales por unica vez...
                                                     estas son:
                                                               1-Crear, si no existe, la base de datos
                                                               2-Crear, si es que no existen, las tablas
                                                               3-Eliminar al Arquelogo 'Benji Colchett' con todos lo objetos asociados a Él
                                                               4-Ingresar al Arqueologo 'Rodolphe Rominov'
                                                     """;
    
    // Objetos utilizados para interactuar con la base de datos
    // (conexión, realizar consultas con y sin parámetros, y recibir los resultados)
    private static Connection conn = null;
    private static Statement query = null;
    private static PreparedStatement p_query = null;
    private static ResultSet result = null;
    
    /**
     * Creates new form DptoArqueologia
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     */
    public DptoArqueologia() throws IOException, SQLException {
        initComponents();
        this.setLocationRelativeTo(null);
        this.setTitle("Departamento de Arqueologia");
        this.setExtendedState(MAXIMIZED_VERT);
        this.setResizable(false);
        JOptionPane.showMessageDialog(null, CREADORES,"Creadores", JOptionPane.PLAIN_MESSAGE);
        
        /*
        Se crea la base de datos en caso de que esta no exista.
        */
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PWD);
            query = conn.createStatement();

            String checkDatabaseQuery = "SELECT COUNT(datname) FROM pg_catalog.pg_database WHERE datname = '"
                    + DB_NAME + "'";
            boolean databaseExists;
            try (var result = query.executeQuery(checkDatabaseQuery)) {
                result.next();
                databaseExists = result.getInt(1) > 0;
            }

            if (!databaseExists) {
                String createDatabaseQuery = "CREATE DATABASE \"" + DB_NAME
                        + "\" WITH OWNER = postgres ENCODING = 'UTF8' TABLESPACE = pg_default CONNECTION LIMIT = -1";
                int res = query.executeUpdate(createDatabaseQuery);

                if (res == 0) {
                    System.out.println("La base de datos se ha creado correctamente.");
                } else {
                    System.err.println("Error al crear la base de datos.");
                }
            } else {
                System.out.println("La base de datos ya existe.");
            }
        } catch (SQLException e) {
            System.err.println("Error al crear la base de datos: " + e.getMessage());
        } finally {
            try {
                if (query != null) {
                    query.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            }
        }
        //PREGUNTAR POR ESTA PARTE
        StringBuffer sb = new StringBuffer();
        sb.append(DB_URL);
        sb.append(DB_NAME);
        conn = DriverManager.getConnection(sb.toString(), DB_USER, DB_PWD);
        query = conn.createStatement();
        
        //CONTROLO SI LA BASE DE DATOS ESTA VACIA O TIENE ELEMENTOS
        result = query.executeQuery("SELECT COUNT(table_name)\n"
                        + "FROM information_schema.tables\n"
                        + "WHERE table_schema='public'\n"
                        + "AND table_type='BASE TABLE';");
        result.next();
        if(result.getInt(1)<7){
            //Llamo al metodo de creacion de tablas ya que la base de datos esta vacia.
            creacionDeTablas();

            //Llamo al metodo de cargado de archivo .sql
            query.execute(cargaArchivo());
            
            //Realizo las operaciones iniciales
            operacionInicial();
        }   
        mostrarPersonas();
        mostrarObjetos();
    }
    
    /*
    Carga la base de datos, instancia inicial.
    */
    private String cargaArchivo() throws FileNotFoundException, IOException{
        String cadena;
        StringBuffer sb = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("src/main/java/com/mycompany/dptoarqueologia/insert/insert.sql"), "utf-8"));
        while((cadena = in.readLine())!=null) {
            //query.execute(cadena);
            sb.append(cadena+"\n");
            //System.out.println(cadena);
        }
        in.close();
        return sb.toString();
    }
    
    /*
    Crea las tablas en la base de datos
    */
    private void creacionDeTablas() throws SQLException{
        // Creacion de la tabla Sitios
        query.execute("CREATE TABLE IF NOT EXISTS Sitios ("
                    + "s_cod VARCHAR(50) PRIMARY KEY NOT NULL ,"
                    + "s_localidad VARCHAR(50) NOT NULL )"
                );
        
        // Creación de la tabla Caudriculas
        query.execute("CREATE TABLE IF NOT EXISTS Cuadriculas ("
                    + "Cu_Cod VARCHAR(50) PRIMARY KEY NOT NULL ,"
                    + "S_Cod_Dividido VARCHAR(50) NOT NULL ,"
                    + "FOREIGN KEY (S_Cod_Dividido) REFERENCES Sitios (s_cod))"
                );
        
        // Creación de la tabla Cajas
        query.execute("CREATE TABLE IF NOT EXISTS Cajas ("
                    + "Ca_Cod VARCHAR(50) PRIMARY KEY NOT NULL ,"
                    + "Ca_Fecha VARCHAR(50) NOT NULL ,"
                    + "Ca_Lugar VARCHAR(50) NOT NULL )"
                );

        // Creación de la tabla Personas   
        query.execute("CREATE TABLE IF NOT EXISTS Personas ("
                    + "P_Dni INT PRIMARY KEY NOT NULL ,"
                    + "P_Nombre VARCHAR(50) NOT NULL ,"
                    + "P_Apellido VARCHAR(50) NOT NULL ,"
                    + "P_Email VARCHAR(50) ,"
                    + "P_Telefono VARCHAR(50) )"
                );

        // Creación de la tabla Objetos
        query.execute("CREATE TABLE IF NOT EXISTS Objetos ("
                    + "O_Cod VARCHAR(50) PRIMARY KEY NOT NULL ,"
                    + "O_Nombre VARCHAR(50) NOT NULL ,"
                    + "O_TipoExtraccion VARCHAR(50) NOT NULL ,"
                    + "O_Alto INT NOT NULL ,"
                    + "O_Largo INT NOT NULL ,"
                    + "O_Espesor INT NOT NULL ,"
                    + "O_Peso INT NOT NULL ,"
                    + "O_Cantidad INT NOT NULL ,"
                    + "O_Fecharegistro DATE NOT NULL ,"
                    + "O_Descripcion VARCHAR(50),"
                    + "O_Origen VARCHAR(50),"
                    + "CU_Cod_Asocia VARCHAR(50),"
                    + "Ca_Cod_Contiene VARCHAR(50),"
                    + "P_Dni_Ingresa INT,"
                    + "O_ES VARCHAR(1)," 
                    + "FOREIGN KEY (CU_Cod_Asocia) REFERENCES Cuadriculas (Cu_Cod),"
                    + "FOREIGN KEY (Ca_Cod_Contiene) REFERENCES Cajas (Ca_Cod),"
                    + "FOREIGN KEY (P_Dni_Ingresa) REFERENCES Personas (P_Dni))"
                );

        // Creación de la tabla Liticos
        query.execute("CREATE TABLE IF NOT EXISTS Liticos ("
                    + "O_cod VARCHAR(50) NOT NULL ,"
                    + "L_Fechacreacion INT,"
                    + "FOREIGN KEY (O_cod) REFERENCES Objetos (O_Cod))"
                );

        // Creación de la tabla Ceramicos
        query.execute("CREATE TABLE IF NOT EXISTS Ceramicos ("
                    + "O_cod VARCHAR(50) NOT NULL ,"
                    + "C_Color Varchar(50) NOT NULL,"
                    + "FOREIGN KEY (O_cod) REFERENCES Objetos (O_Cod))"
                );
    }
    /*
    Operacion inicial, Incorpora un nuevo arqueologo "Rodolphe Rominov"
    y elimina al arqueologo "Benji Colchett".
    */
    private void operacionInicial() throws SQLException{
        // Se inserta al Investigador Rodolphe Rominov 
        query.execute("INSERT INTO Personas (P_Dni, P_Nombre, P_Apellido, P_Email, P_Telefono) values (25544555, 'Rodolphe', 'Rominov', 'rrominovm@sciencedaily.com', '7135986253')");
       
        /*
        Se elimina al Investigador Benji Colchett:
        Primero eliminamos todos los objetos relacionados con "Benji Colchett", para ello primero debemos obtener el dni
        de "Benji Colchett", para luego hacer un delete sobre todos los objetos cuyo campo p_dni_ingresa son iguales que el del
        investigador anterior.
        Luego eliminamos al investigador.
        */
        int dni = 0;        
        
        result = query.executeQuery("SELECT p_dni FROM Personas where P_Nombre = 'Benji' and P_Apellido = 'Colchett'");
        if(result.next())
            dni = result.getInt("p_dni");
        
        result = query.executeQuery("SELECT O_cod FROM Objetos where P_dni_ingresa = "+ dni+" and O_es = 'L'");
        List<String> codigosObLit = new ArrayList<>();
        while (result.next()) {
            String codigoObjeto = result.getString("O_cod");
            codigosObLit.add(codigoObjeto);
        }
        for (String codigoObjeto : codigosObLit) 
            //Eliminar registros en la tabla Liticos
            query.executeUpdate("DELETE FROM Liticos WHERE O_cod = '"+ codigoObjeto+"'");
        
        result = query.executeQuery("SELECT O_cod FROM Objetos where P_dni_ingresa = "+ dni+" and O_es = 'C'");
        List<String> codigosObCer = new ArrayList<>();
        while (result.next()) {
            String codigoObjeto = result.getString("O_cod");
            codigosObCer.add(codigoObjeto);
        }
        for (String codigoObjeto : codigosObCer) 
            //Eliminar registros en la tabla Ceramicos
            query.executeUpdate("DELETE FROM Ceramicos WHERE O_cod = '"+ codigoObjeto+"'");

        query.execute("DELETE FROM Objetos where p_dni_ingresa = "+ dni );
        query.execute("DELETE FROM Personas where p_dni = "+ dni);
        // JFrame que nos avisara, que se eliminaron los objetos relacionados al investigador eliminado
        JOptionPane.showMessageDialog(null, MENSAJE_BIENVENIDA);
        //JOptionPane.showConfirmDialog(null,"Se elimino al Arqueologo 'Benji Colchett' y se eliminaron todos los objetos relacionados con El","Arqueologo Eliminado",JOptionPane.PLAIN_MESSAGE);
        //JOptionPane.showConfirmDialog(null,"Se Agrego al Arqueologo Rodolphe Rominov","Ingreso Arqueologo",JOptionPane.PLAIN_MESSAGE);
    }
    /*
    Elimina una caja mediante el codigo de la caja.
    */
    private void eliminarCaja() throws SQLException{
        String codStr = JOptionPane.showInputDialog(null, "Ingrese el codigo de la caja que desea eliminar: ", "Eliminar caja",JOptionPane.PLAIN_MESSAGE);
        if(codStr == null){
            // El usuario cancelo la operacion
        }else{
            //El usuario acepto la operacion
            try {
                // Verificar si la caja existe, sino da una excepcion
                ResultSet resultCaja = query.executeQuery("SELECT ca_cod FROM Cajas WHERE ca_cod = '" + codStr + "'");
                if (resultCaja.next()) {
                    // La caja existe
                    mostrarCaja(codStr);

                    ResultSet resultObjetos = query.executeQuery("SELECT o_cod FROM Objetos WHERE ca_cod_contiene = '" + codStr + "'");
                    if (resultObjetos.next()) {
                        // La caja contiene objetos, no se puede eliminar
                        JOptionPane.showMessageDialog(null, "Esta Caja contiene Objetos y no se puede eliminar.", "Aviso", JOptionPane.PLAIN_MESSAGE);
                    } else {
                        // La caja no contiene objetos, se puede eliminar
                        query.executeUpdate("DELETE FROM Cajas WHERE ca_cod = '" + codStr + "'");
                        JOptionPane.showMessageDialog(null, "Se eliminó la Caja de Código '" + codStr + "'.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    // La caja no existe
                    JOptionPane.showMessageDialog(null, "La Caja con Código " + codStr + " no existe y no pudo ser eliminada.", "Error", JOptionPane.WARNING_MESSAGE);
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Ocurrió un error al intentar eliminar la Caja con Código " + codStr + ".", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private static DefaultTableModel resultToTable(ResultSet rs) throws SQLException {
        // Esta es una función auxiliar que les permite convertir los resultados de las
        // consultas (ResultSet) a un modelo interpretable para la tabla mostrada en pantalla
        ResultSetMetaData metaData = rs.getMetaData();

        // creando las culmnas de la tabla
        Vector<String> columnNames = new Vector<String>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
        }

        // creando las filas de la tabla con los resultados de la consulta
        Vector<Vector<Object>> data = new Vector<Vector<Object>>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<Object>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
            }
            data.add(vector);
        }

        return new DefaultTableModel(data, columnNames);
    }
    
    /*
    Muestra la instancia de Personas.
    */
    private void mostrarPersonas() throws SQLException{
        query = conn.createStatement();
        result = query.executeQuery("select * from personas order by p_apellido");
        tablaPersonas.setModel(resultToTable(result));
    }
    
    /*
    Muestra la instancia de Objetos.
    */
    private void mostrarObjetos() throws SQLException{
        query = conn.createStatement();
        result = query.executeQuery("select * from objetos");
        tablaObjetos.setModel(resultToTable(result));
    }
    
    /*
    Muestra los Objetos dentro de una Caja a la que pertenecen.
    */
    private void mostrarCaja(String str) throws SQLException{
        query = conn.createStatement();
        result = query.executeQuery("select * from objetos where ca_cod_contiene = '"+str+"'");
        tablaOperaciones.setModel(resultToTable(result));
    }
    
    /*
    Actualiza las tablas.
    */
    private void updateForm() throws SQLException {
        // actualizar y limpiar el formulario luego de una operación exitosa
        jtCodigoO.setText("");
        jtNombreO.setText("");
        jtTipoO.setText("");
        jsAltoO.setValue(1);
        jsLargoO.setValue(1);
        jsEspesorO.setValue(1);
        jsPesoO.setValue(1);
        jsCantidadO.setValue(1);
        jtDescripcionO.setText("");
        jtOrigenO.setText("");
        jsCuadriculaO.setValue(1);
        jsCodigoCO.setValue(1);
        jsDNIAO.setValue(1000000);
        
        mostrarPersonas();
        mostrarObjetos();
    }
    
    /*
    Agrega un Objeto a la base de datos.
    */
    private void insertaObjeto() throws SQLException{
        String opcion = "";
        String codigoObjeto = jtCodigoO.getText().trim();
        p_query= conn.prepareStatement("SELECT O_Cod FROM Objetos WHERE O_Cod = ?");
        p_query.setString(1, codigoObjeto);
        result = p_query.executeQuery();

        if (result.next()) {
            JOptionPane.showConfirmDialog(null, "El código del objeto ya existe", "Aviso", JOptionPane.PLAIN_MESSAGE);
        } else {
            p_query = conn.prepareStatement("insert into Objetos (O_Cod, O_Nombre, O_Tipoextraccion, O_Alto, O_Largo, O_Espesor, O_Peso, O_Cantidad, O_Fecharegistro, O_Descripcion, O_Origen, CU_Cod_Asocia, CA_Cod_Contiene, P_Dni_Ingresa, O_Es) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            if(jtCodigoO.getText().equals("")||jtNombreO.getText().equals("")||jtTipoO.getText().equals("")||jdFechaRegistro.getDateFormatString().equals("")||jtDescripcionO.getText().equals("")||jtOrigenO.getText().equals("")){
                JOptionPane.showConfirmDialog(null,"Faltan Ingresar Campos","Aviso",JOptionPane.PLAIN_MESSAGE);
            }else{
                result = query.executeQuery("select count (p_dni) as cant_personas from personas where p_dni = "+(int) jsDNIAO.getValue());
                result.next();
                if(result.getInt(1) != 1){
                    JOptionPane.showConfirmDialog(null,"El DNI ingresado no corresponde a un Arqueologo existente","Aviso",JOptionPane.PLAIN_MESSAGE);
                }else{                
                    p_query.setString(1,codigoObjeto);       
                    p_query.setString(2, jtNombreO.getText().trim()); 
                    p_query.setString(3, jtTipoO.getText().trim());        
                    p_query.setInt(4, (int) jsAltoO.getValue());
                    p_query.setInt(5, (int) jsLargoO.getValue());
                    p_query.setInt(6, (int) jsEspesorO.getValue());
                    p_query.setInt(7, (int) jsPesoO.getValue());
                    p_query.setInt(8, (int) jsCantidadO.getValue());
                    p_query.setDate(9, new java.sql.Date(jdFechaRegistro.getDate().getTime()));
                    p_query.setString(10, jtDescripcionO.getText().trim());
                    p_query.setString(11, jtOrigenO.getText().trim());
                    p_query.setString(12,"CU"+jsCuadriculaO.getValue());
                    p_query.setString(13, "CA"+jsCodigoCO.getValue());
                    p_query.setInt(14, (int) jsDNIAO.getValue());
                    p_query.setString(15, jcEspecificacionO.getSelectedItem().toString());
                    //Ingreso el objeto
                    updateForm();
                    p_query.executeUpdate();
                    if (jcEspecificacionO.getSelectedItem().toString().equals("C")) {
                        PreparedStatement p_queryC = conn.prepareStatement("insert into Ceramicos (O_Cod, C_Color) values (?, ?)");
                        opcion = JOptionPane.showInputDialog(null, "Ingrese Color: ", "Tipo Ceramico", JOptionPane.PLAIN_MESSAGE);
                        while (opcion.equals("")) {
                            JOptionPane.showConfirmDialog(null, "Debe ingresar el Color", "Aviso", JOptionPane.PLAIN_MESSAGE);
                            opcion = JOptionPane.showInputDialog(null, "Ingrese Color: ", "Tipo Ceramico", JOptionPane.PLAIN_MESSAGE);
                        }
                        // Ingreso el Ceramico
                        p_queryC.setString(1, codigoObjeto);
                        p_queryC.setString(2, opcion);
                        p_queryC.executeUpdate();  // Ejecutar la consulta de inserción
                    } else {
                        PreparedStatement p_queryL = conn.prepareStatement("insert into Liticos (O_Cod, L_fechacreacion) values (?, ?)");
                        opcion = JOptionPane.showInputDialog(null, "Ingrese Fecha Creacion: ", "Tipo Litico", JOptionPane.PLAIN_MESSAGE);
                        while (opcion.equals("")) {
                            JOptionPane.showConfirmDialog(null, "Debe ingresar la Fecha de Creacion", "Aviso", JOptionPane.PLAIN_MESSAGE);
                            opcion = JOptionPane.showInputDialog(null, "Ingrese Fecha Creacion: ", "Tipo Litico", JOptionPane.PLAIN_MESSAGE);
                        }
                        // Ingreso el Litico
                        p_queryL.setString(1, codigoObjeto);
                        p_queryL.setInt(2, Integer.parseInt(opcion));
                        p_queryL.executeUpdate();  // Ejecutar la consulta de inserción
                    }
                    JOptionPane.showConfirmDialog(null, "Se ingreso correctamente", "Aviso", JOptionPane.PLAIN_MESSAGE);
                }            
            }
        }
    }
    /*
    Busca un Objeto por su codigo.
    */  
    private void buscarObjeto() throws SQLException{
        if(jsCodigoOB.getValue().equals("")){
            JOptionPane.showConfirmDialog(null,"Falta Completar Campo Codigo","Aviso",JOptionPane.PLAIN_MESSAGE);
        }else{
            
            result = query.executeQuery("select count (ca_cod_contiene) as cant_objetos from objetos where ca_cod_contiene = 'CA"+jsCodigoOB.getValue()+"'");
            result.next();
            if(result.getInt(1) == 0){
                JOptionPane.showConfirmDialog(null,"Esta Caja No contiene Objetos","Aviso",JOptionPane.PLAIN_MESSAGE);
            }else{
                query = conn.createStatement();
                result = query.executeQuery("select * from objetos where ca_cod_contiene = 'CA"+jsCodigoOB.getValue()+"'");
                tablaBuscarO.setModel(resultToTable(result));                              
            }
        }
    }
    
    /*
    Muestra las cantidades de Cajas, Personas, Objetos, Cuadriculas, en la instancia actual de la base de datos.
    */
    private void cantidadTotalActual() throws SQLException{
        int cantCajas, cantPersonas, cantObjetos, cantCuadriculas;
        result = query.executeQuery("select count (ca_cod) as cantidad_cajas from cajas");
        result.next();
        cantCajas = result.getInt(1);
        result = query.executeQuery("select count (p_dni) as cantidad_personas from personas");
        result.next();
        cantPersonas = result.getInt(1);
        result = query.executeQuery("select count (o_cod) as cantidad_objetos from objetos");
        result.next();
        cantObjetos = result.getInt(1);
        result = query.executeQuery("select count (cu_cod) as cantidad_cuadriculas from cuadriculas");
        result.next();
        cantCuadriculas = result.getInt(1);
        JOptionPane.showMessageDialog(null, "Cantidad de Personas: "+cantPersonas+"\nCantidad de Cajas: "+cantCajas+"\nCantidad de Cuadriculas: "+cantCuadriculas+"\nCantidad de Objetos: "+cantObjetos, "Cantidad Actual", JOptionPane.PLAIN_MESSAGE);
    }
    
    /*
    Muestra la cantidad total de Objetos Liticos y Ceramicos en la instancia actual.
    */
    private void cantidadTotalLC() throws SQLException{
        int cantLiticos, cantCeramicos;
        result = query.executeQuery("select count (o_cod) as cantidad_liticos from liticos");
        result.next();
        cantLiticos = result.getInt(1);
        result = query.executeQuery("select count (o_cod) as cantidad_liticos from ceramicos");
        result.next();
        cantCeramicos = result.getInt(1);        
        JOptionPane.showMessageDialog(null, "Cantidad de Liticos: "+cantLiticos+"\nCantidad de Ceramicos: "+cantCeramicos, "Cantidad Actual de Liticos y Ceramicos", JOptionPane.PLAIN_MESSAGE);
    }
    
    /*
    Muestra informacion sobre los pesos de los Objetos.
    */
    private void estadisticasObjeto() throws SQLException{
        float promPeso;
        int pesoMax, pesoMin;
        result = query.executeQuery("select avg(o_peso) from objetos");
        result.next();
        promPeso = result.getFloat(1);
        result = query.executeQuery("select min(o_peso) from objetos");
        result.next();
        pesoMin = (int) result.getInt(1);
        result = query.executeQuery("select max(o_peso) from objetos");
        result.next();
        pesoMax = (int) result.getInt(1);
        
        JOptionPane.showMessageDialog(null, "Promedio Peso: "+promPeso+"\nPeso Maximo: "+pesoMax+"\nPeso Minimo: "+pesoMin, "Promedio, Maximo y Minimo Peso de los Objetos", JOptionPane.PLAIN_MESSAGE);
    }
    
    /*
    Lista a los Arqueologos y a sus cantidades de objetos encontrados.
    Si el arqueologo no tiene algun objeto asignado, no se mostrara.
    */
    private void cantidadOA() throws SQLException{
        query = conn.createStatement();
        result = query.executeQuery("select p_nombre,p_apellido,count(p_dni) as cantidad_objetos from personas,objetos where p_dni=p_dni_ingresa group by p_dni order by p_apellido");
        tablaOperaciones.setModel(resultToTable(result)); 
    }
    
    /*
    Lista las Cajas con sus respectivo peso, en base a los objetos que estan dentro de ella.
    */
    private void pesoCajas() throws SQLException{
        query = conn.createStatement();
        result = query.executeQuery("select ca_cod_contiene, sum(o_peso) as peso_caja from objetos group by ca_cod_contiene order by ca_cod_contiene");
        tablaOperaciones.setModel(resultToTable(result)); 
    }
    /*
    Lista las Cajas vacias.
    */
    private void cajasVacias() throws SQLException{
        /*FALTA PONER EL LUGAR*/
        query = conn.createStatement();
        result = query.executeQuery("select ca_lugar,ca_cod from cajas where ca_cod in (select ca_cod from cajas except select ca_cod_contiene from objetos group by ca_cod_contiene)");
        tablaOperaciones.setModel(resultToTable(result));
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jArqueologos = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaPersonas = new javax.swing.JTable();
        jObjetos = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tablaObjetos = new javax.swing.JTable();
        jNuevoObjeto = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jtCodigoO = new javax.swing.JTextField();
        jtTipoO = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jsAltoO = new javax.swing.JSpinner();
        jsLargoO = new javax.swing.JSpinner();
        jsPesoO = new javax.swing.JSpinner();
        jsEspesorO = new javax.swing.JSpinner();
        jsCantidadO = new javax.swing.JSpinner();
        jtDescripcionO = new javax.swing.JTextField();
        jtOrigenO = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jsDNIAO = new javax.swing.JSpinner();
        jcEspecificacionO = new javax.swing.JComboBox<>();
        jbInsertarO = new javax.swing.JButton();
        jtNombreO = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jsCodigoCO = new javax.swing.JSpinner();
        jsCuadriculaO = new javax.swing.JSpinner();
        jdFechaRegistro = new com.toedter.calendar.JDateChooser();
        jBuscarObjetos = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        jbBuscarO = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        tablaBuscarO = new javax.swing.JTable();
        jsCodigoOB = new javax.swing.JSpinner();
        jOtrasOperaciones = new javax.swing.JPanel();
        jbCantidadActual = new javax.swing.JButton();
        jbCantidadLC = new javax.swing.JButton();
        jbEstadisticasO = new javax.swing.JButton();
        jbPesoCajas = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        tablaOperaciones = new javax.swing.JTable();
        jbCantidadOA = new javax.swing.JButton();
        jbCajasVacias = new javax.swing.JButton();
        jbEliminarCaja = new javax.swing.JToggleButton();
        jbEntreFechas = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTabbedPane1.setPreferredSize(new java.awt.Dimension(1103, 700));

        tablaPersonas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaPersonas.setEnabled(false);
        jScrollPane1.setViewportView(tablaPersonas);

        javax.swing.GroupLayout jArqueologosLayout = new javax.swing.GroupLayout(jArqueologos);
        jArqueologos.setLayout(jArqueologosLayout);
        jArqueologosLayout.setHorizontalGroup(
            jArqueologosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1200, Short.MAX_VALUE)
        );
        jArqueologosLayout.setVerticalGroup(
            jArqueologosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jArqueologosLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 700, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Arqueologos", jArqueologos);

        tablaObjetos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaObjetos.setEnabled(false);
        jScrollPane2.setViewportView(tablaObjetos);

        javax.swing.GroupLayout jObjetosLayout = new javax.swing.GroupLayout(jObjetos);
        jObjetos.setLayout(jObjetosLayout);
        jObjetosLayout.setHorizontalGroup(
            jObjetosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1200, Short.MAX_VALUE)
        );
        jObjetosLayout.setVerticalGroup(
            jObjetosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jObjetosLayout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 700, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 19, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Objetos", jObjetos);

        jNuevoObjeto.setPreferredSize(new java.awt.Dimension(1103, 660));

        jLabel1.setText("Codigo:");

        jLabel2.setText("Nombre:");

        jLabel6.setText("Tipo Extracción:");

        jLabel7.setText("Alto:");

        jLabel8.setText("Largo:");

        jLabel9.setText("Espesor:");

        jLabel10.setText("Peso:");

        jLabel11.setText("Cantidad:");

        jLabel12.setText("Fecha Registro:");

        jLabel13.setText("Descripción:");

        jLabel14.setText("Origen:");

        jsAltoO.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));

        jsLargoO.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));

        jsPesoO.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));

        jsEspesorO.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));

        jsCantidadO.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));

        jLabel3.setText("Codigo Caja:");

        jLabel15.setText("DNI del Arqueologo:");

        jLabel16.setText("Especificacion Objeto:");

        jsDNIAO.setModel(new javax.swing.SpinnerNumberModel(1000000, 1000000, 200000000, 1));

        jcEspecificacionO.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "C", "L" }));

        jbInsertarO.setText("Agregar");
        jbInsertarO.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbInsertarOActionPerformed(evt);
            }
        });

        jLabel5.setText("Numero Cuadricula:");

        jsCodigoCO.setModel(new javax.swing.SpinnerNumberModel(1, 1, 26, 1));

        jsCuadriculaO.setModel(new javax.swing.SpinnerNumberModel(1, 1, 50, 1));

        javax.swing.GroupLayout jNuevoObjetoLayout = new javax.swing.GroupLayout(jNuevoObjeto);
        jNuevoObjeto.setLayout(jNuevoObjetoLayout);
        jNuevoObjetoLayout.setHorizontalGroup(
            jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                .addGap(0, 168, Short.MAX_VALUE)
                .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(jtCodigoO, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(jtNombreO, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(18, 18, 18)
                        .addComponent(jsAltoO, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addGap(18, 18, 18)
                        .addComponent(jsLargoO, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addGap(18, 18, 18)
                        .addComponent(jsEspesorO, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addGap(18, 18, 18)
                        .addComponent(jsPesoO, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addGap(18, 18, 18)
                        .addComponent(jdFechaRegistro, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addGap(18, 18, 18)
                        .addComponent(jsCantidadO, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(287, 287, 287)
                .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                        .addGap(78, 78, 78)
                        .addComponent(jLabel14)
                        .addGap(18, 18, 18)
                        .addComponent(jtOrigenO, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                        .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel13)
                            .addComponent(jLabel16)
                            .addComponent(jLabel15)
                            .addComponent(jLabel3)
                            .addComponent(jLabel6)
                            .addComponent(jLabel5))
                        .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jsCodigoCO))
                            .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jsDNIAO))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jNuevoObjetoLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jcEspecificacionO, 0, 159, Short.MAX_VALUE)
                                    .addComponent(jtDescripcionO, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE)
                                    .addComponent(jtTipoO)
                                    .addComponent(jsCuadriculaO, javax.swing.GroupLayout.Alignment.TRAILING))))))
                .addContainerGap(198, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jNuevoObjetoLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jbInsertarO, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(513, 513, 513))
        );
        jNuevoObjetoLayout.setVerticalGroup(
            jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                .addContainerGap(52, Short.MAX_VALUE)
                .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jtOrigenO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel14)
                        .addComponent(jtCodigoO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1)))
                .addGap(18, 18, 18)
                .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jsCodigoCO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtNombreO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(18, 18, 18)
                .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jsDNIAO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)
                    .addComponent(jsAltoO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addGap(18, 18, 18)
                .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jcEspecificacionO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16)
                    .addComponent(jsLargoO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addGap(18, 18, 18)
                .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jsEspesorO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addGap(33, 33, 33)
                .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jsPesoO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(jtDescripcionO, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13))
                .addGap(18, 18, 18)
                .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jNuevoObjetoLayout.createSequentialGroup()
                        .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jsCantidadO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel11)
                            .addComponent(jtTipoO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel12)
                            .addComponent(jdFechaRegistro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jNuevoObjetoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jsCuadriculaO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel5)))
                        .addGap(18, 18, 18)
                        .addComponent(jbInsertarO, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel6))
                .addGap(280, 280, 280))
        );

        jTabbedPane1.addTab("Nuevo Objeto", jNuevoObjeto);

        jLabel17.setText("Codigo de caja:");

        jbBuscarO.setText("Listar Objetos");
        jbBuscarO.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbBuscarOActionPerformed(evt);
            }
        });

        tablaBuscarO.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaBuscarO.setEnabled(false);
        jScrollPane3.setViewportView(tablaBuscarO);

        jsCodigoOB.setModel(new javax.swing.SpinnerNumberModel(1, 1, 26, 1));

        javax.swing.GroupLayout jBuscarObjetosLayout = new javax.swing.GroupLayout(jBuscarObjetos);
        jBuscarObjetos.setLayout(jBuscarObjetosLayout);
        jBuscarObjetosLayout.setHorizontalGroup(
            jBuscarObjetosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jBuscarObjetosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 1188, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jBuscarObjetosLayout.createSequentialGroup()
                .addContainerGap(437, Short.MAX_VALUE)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jsCodigoOB, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jbBuscarO, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(360, 360, 360))
        );
        jBuscarObjetosLayout.setVerticalGroup(
            jBuscarObjetosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jBuscarObjetosLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jBuscarObjetosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbBuscarO, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)
                    .addComponent(jsCodigoOB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 615, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(32, Short.MAX_VALUE))
        );

        jbBuscarO.getAccessibleContext().setAccessibleName("Buscar Caja");

        jTabbedPane1.addTab("Buscar Caja", jBuscarObjetos);

        jbCantidadActual.setText("Cantidad Actual");
        jbCantidadActual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbCantidadActualActionPerformed(evt);
            }
        });

        jbCantidadLC.setText("Cantidad Liticos y Ceramicos");
        jbCantidadLC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbCantidadLCActionPerformed(evt);
            }
        });

        jbEstadisticasO.setText("Estadisticas Peso Objetos");
        jbEstadisticasO.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbEstadisticasOActionPerformed(evt);
            }
        });

        jbPesoCajas.setText("Peso por Cajas");
        jbPesoCajas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbPesoCajasActionPerformed(evt);
            }
        });

        tablaOperaciones.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaOperaciones.setEnabled(false);
        jScrollPane5.setViewportView(tablaOperaciones);

        jbCantidadOA.setText("Cant. de Objetos por Arqueologo");
        jbCantidadOA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbCantidadOAActionPerformed(evt);
            }
        });

        jbCajasVacias.setText("Cajas Vacias");
        jbCajasVacias.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbCajasVaciasActionPerformed(evt);
            }
        });

        jbEliminarCaja.setText("Eliminar Caja");
        jbEliminarCaja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbEliminarCajaActionPerformed(evt);
            }
        });

        jbEntreFechas.setText("Buscar Obj. Por Fechas");
        jbEntreFechas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbEntreFechasActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jOtrasOperacionesLayout = new javax.swing.GroupLayout(jOtrasOperaciones);
        jOtrasOperaciones.setLayout(jOtrasOperacionesLayout);
        jOtrasOperacionesLayout.setHorizontalGroup(
            jOtrasOperacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jOtrasOperacionesLayout.createSequentialGroup()
                .addGap(44, 44, 44)
                .addGroup(jOtrasOperacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jOtrasOperacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jOtrasOperacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jbCajasVacias, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jbEliminarCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jbCantidadActual, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jbCantidadLC, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jbEstadisticasO, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jbPesoCajas, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jbCantidadOA, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jbEntreFechas, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(29, 29, 29)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 890, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(22, Short.MAX_VALUE))
        );
        jOtrasOperacionesLayout.setVerticalGroup(
            jOtrasOperacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jOtrasOperacionesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jOtrasOperacionesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 690, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jOtrasOperacionesLayout.createSequentialGroup()
                        .addComponent(jbEliminarCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jbCantidadActual, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jbCantidadLC, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jbEstadisticasO, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jbPesoCajas, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jbCantidadOA, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jbCajasVacias, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jbEntreFechas, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(23, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Otras Operaciones", jOtrasOperaciones);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 750, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jTabbedPane1.getAccessibleContext().setAccessibleDescription("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jbCajasVaciasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbCajasVaciasActionPerformed
        try {
            // TODO add your handling code here:
            cajasVacias();
        } catch (SQLException ex) {
            Logger.getLogger(DptoArqueologia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jbCajasVaciasActionPerformed

    private void jbCantidadOAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbCantidadOAActionPerformed
        try {
            // TODO add your handling code here:
            cantidadOA();
        } catch (SQLException ex) {
            Logger.getLogger(DptoArqueologia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jbCantidadOAActionPerformed

    private void jbPesoCajasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbPesoCajasActionPerformed
        try {
            // TODO add your handling code here:
            pesoCajas();
        } catch (SQLException ex) {
            Logger.getLogger(DptoArqueologia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jbPesoCajasActionPerformed

    private void jbEstadisticasOActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbEstadisticasOActionPerformed
        try {
            // TODO add your handling code here:
            estadisticasObjeto();
        } catch (SQLException ex) {
            Logger.getLogger(DptoArqueologia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jbEstadisticasOActionPerformed

    private void jbCantidadLCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbCantidadLCActionPerformed
        try {
            // TODO add your handling code here:
            cantidadTotalLC();
        } catch (SQLException ex) {
            Logger.getLogger(DptoArqueologia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jbCantidadLCActionPerformed

    private void jbCantidadActualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbCantidadActualActionPerformed
        try {
            // TODO add your handling code here:
            cantidadTotalActual();
        } catch (SQLException ex) {
            Logger.getLogger(DptoArqueologia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jbCantidadActualActionPerformed

    private void jbBuscarOActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbBuscarOActionPerformed
        
        try {
            // TODO add your handling code here:
            buscarObjeto();
            mostrarPersonas();
            mostrarObjetos();
        } catch (SQLException ex) {
            Logger.getLogger(DptoArqueologia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jbBuscarOActionPerformed

    private void jbEliminarCajaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbEliminarCajaActionPerformed
        // TODO add your handling code here:
        try {
            eliminarCaja();
        } catch (SQLException ex) {
            Logger.getLogger(DptoArqueologia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jbEliminarCajaActionPerformed

    private void jbEntreFechasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbEntreFechasActionPerformed
        // TODO add your handling code here:
        SimpleDateFormat formateador = new SimpleDateFormat("dd-MM-yyyy");
        String fecha1, fecha2;

        // Crea los componentes JDateChooser
        JDateChooser date1 = new JDateChooser();
        JDateChooser date2 = new JDateChooser();

        // Crear un array de Object para almacenar los componentes
        Object[] components = { "Desde:", date1, "Hasta:", date2 };
        int resultado = JOptionPane.showConfirmDialog(null, components, "Ingresar Fechas", JOptionPane.OK_CANCEL_OPTION);

        // Obtener las fechas seleccionadas si se presionó el botón OK
        if (resultado == JOptionPane.OK_OPTION) {
            fecha1 = formateador.format(date1.getDate());
            fecha2 = formateador.format(date2.getDate());

            if (fecha1.equals(fecha2) || date2.getDate().after(date1.getDate())) {
                try {
                    p_query = conn.prepareStatement("SELECT o_cod, o_nombre " +
                                                    "FROM Objetos " +
                                                    "WHERE o_fecharegistro BETWEEN ? AND ?");
                    p_query.setDate(1, new java.sql.Date(date1.getDate().getTime()));
                    p_query.setDate(2, new java.sql.Date(date2.getDate().getTime()));
                    result = p_query.executeQuery();


                    if (result.next()) {
                        DefaultTableModel model = resultToTable(result);
                        tablaOperaciones.setModel(model);
                    } else {
                        // No se encontraron objetos entre las fechas seleccionadas
                        JOptionPane.showMessageDialog(null, "No se han encontrado objetos entre las fechas: " + fecha1 + " y " + fecha2 + ".", "Información", JOptionPane.INFORMATION_MESSAGE);
                        tablaOperaciones.setModel(new DefaultTableModel()); // Limpiar la tabla
                    }

                    date1.setDate(null);
                    date2.setDate(null);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null, "Error al ejecutar la consulta.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "La fecha de inicio (desde) debe ser menor o igual a la fecha final (hasta).", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jbEntreFechasActionPerformed

    private void jbInsertarOActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbInsertarOActionPerformed

        try {
            // TODO add your handling code here:
            insertaObjeto();
            mostrarPersonas();
            mostrarObjetos();
        } catch (SQLException ex) {
            Logger.getLogger(DptoArqueologia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jbInsertarOActionPerformed
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel( new FlatMacDarkLaf() );
        } catch( Exception ex ) {
            System.err.println( "Failed to initialize LaF" );
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new DptoArqueologia().setVisible(true);
                } catch (IOException ex) {
                    Logger.getLogger(DptoArqueologia.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SQLException ex) {
                    Logger.getLogger(DptoArqueologia.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jArqueologos;
    private javax.swing.JPanel jBuscarObjetos;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jNuevoObjeto;
    private javax.swing.JPanel jObjetos;
    private javax.swing.JPanel jOtrasOperaciones;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JButton jbBuscarO;
    private javax.swing.JButton jbCajasVacias;
    private javax.swing.JButton jbCantidadActual;
    private javax.swing.JButton jbCantidadLC;
    private javax.swing.JButton jbCantidadOA;
    private javax.swing.JToggleButton jbEliminarCaja;
    private javax.swing.JButton jbEntreFechas;
    private javax.swing.JButton jbEstadisticasO;
    private javax.swing.JButton jbInsertarO;
    private javax.swing.JButton jbPesoCajas;
    private javax.swing.JComboBox<String> jcEspecificacionO;
    private com.toedter.calendar.JDateChooser jdFechaRegistro;
    private javax.swing.JSpinner jsAltoO;
    private javax.swing.JSpinner jsCantidadO;
    private javax.swing.JSpinner jsCodigoCO;
    private javax.swing.JSpinner jsCodigoOB;
    private javax.swing.JSpinner jsCuadriculaO;
    private javax.swing.JSpinner jsDNIAO;
    private javax.swing.JSpinner jsEspesorO;
    private javax.swing.JSpinner jsLargoO;
    private javax.swing.JSpinner jsPesoO;
    private javax.swing.JTextField jtCodigoO;
    private javax.swing.JTextField jtDescripcionO;
    private javax.swing.JTextField jtNombreO;
    private javax.swing.JTextField jtOrigenO;
    private javax.swing.JTextField jtTipoO;
    private javax.swing.JTable tablaBuscarO;
    private javax.swing.JTable tablaObjetos;
    private javax.swing.JTable tablaOperaciones;
    private javax.swing.JTable tablaPersonas;
    // End of variables declaration//GEN-END:variables
}
