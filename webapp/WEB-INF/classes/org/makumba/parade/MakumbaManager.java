package org.makumba.parade;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class MakumbaManager {
    static {
        // force a class load
        Class c = org.makumba.MakumbaSystem.class;
    }

    public String getMakumbaVersion(String p) {
        final String path = p;
        String version = "unknown";

        try {
            File fl = new File((path + "/WEB-INF/lib/makumba.jar").replace('/',
                    File.separatorChar));
            JarFile jar = new JarFile(fl);
            Manifest mf = jar.getManifest();
            Attributes att = mf.getAttributes("Makumba");
            version = att.getValue("Version");
            jar.close();
            return version;
        } catch (Exception e) {
            // when no version info is inside JAR's META-INF/manifest.mf file
            return getMakumbaVersionOld(p);
        }
    }

    private String getMakumbaVersionOld(String p) {
        final String path = p;
        Class c = null;

        try {
            c = new ClassLoader() {
                public Class findClass(String name)
                        throws ClassNotFoundException {
                    try {
                        File fl = new File((path + "/WEB-INF/lib/makumba.jar")
                                .replace('/', File.separatorChar));
                        if (!fl.exists())
                            throw new ClassNotFoundException("Jar file " + fl
                                    + " not found");
                        JarFile f = new JarFile(fl);
                        String nm = name.replace('.', '/') + ".class";
                        JarEntry j = f.getJarEntry(nm);
                        // System.out.println(fl +" "+fl.exists()+" "+nm+" "+j);

                        // JarURLConnection conn= (JarURLConnection)(new
                        // URL("jar:file:"+path+"/WEB-INF/lib/makumba.jar!/"+name.replace('.',
                        // '/')+".class")).openConnection();
                        // conn.setUseCaches(false);
                        // for(Enumeration e=f.entries(); e.hasMoreElements();
                        // )System.out.println("bla" +e.nextElement());
                        // InputStream i=
                        // conn.getJarFile().getInputStream(conn.getJarEntry());
                        InputStream i = f.getInputStream(j);
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int n;
                        while ((n = i.read(buffer)) != -1)
                            bo.write(buffer, 0, n);
                        i.close();
                        f.close();
                        byte b[] = bo.toByteArray();
                        return defineClass(name, b, 0, b.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name);
                    }
                }
            }.findClass("org.makumba.MakumbaSystem");
        } catch (ClassNotFoundException e) {
            if (e.getMessage().startsWith("Jar file"))
                return "no makumba.jar";
            return "invalid jar";
        }
        Method m = null;
        try {
            m = c.getMethod("getVersion", new Class[0]);
        } catch (NoSuchMethodException nsme) {
            return "< 0.5.0.10";
        }

        String ret = null;
        try {
            ret = (String) m.invoke(null, new Object[0]);
        } catch (Throwable t) {
            throw new RuntimeException("did not expect " + t);
        }

        if (ret.startsWith("makumba-") || ret.startsWith("makumba_"))
            return ret.substring("makumba-".length()).replace('_', '.');
        if (ret.length() == 0)
            return "development version";
        return ret;
    }

    public void setParadeSimple(java.util.Map row,
            javax.servlet.jsp.PageContext pc) {
        // for now we assume only makumba apps that are servlet contenxts
        if (row.get("servletContext.path") == null)
            return;
        String root = row.get("parade.path") + File.separator
                + row.get("servletContext.path");
        row.put("makumba.version", getMakumbaVersion(root));
        root = (root + "/WEB-INF/classes/").replace('/', File.separatorChar);
        File f = new File(root + "MakumbaDatabase.properties");
        if (!f.exists())
            return;
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(f));
        } catch (IOException e) {
            row.put("makumba.database", "invalid MakumbaDatabase.properties");
            return;
        }

        String db = org.makumba.db.Database.findDatabaseName(p);
        row.put("makumba.database", db);
        p.clear();
        try {
            p.load(new FileInputStream(root + db + ".properties"));
        } catch (IOException e) {
            row.put("makumba.database", "cannot find " + db);
            return;
        }
        row.put("makumba.dbsv", p.get("dbsv"));
    }

    public static void response(javax.servlet.jsp.PageContext pageContext) {
        // Responder.response(pageContext.getRequest(),
        // pageContext.getResponse());
    }

    public static void main(String argv[]) {
        System.out.println(new MakumbaManager().getMakumbaVersion(argv[0]));
    }
}
