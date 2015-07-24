package org.keycloak.services.resources.admin;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;

import org.jboss.logging.Logger;
import org.keycloak.Version;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.provider.Spi;

/**
 * REST endpoint which return info for "Server Info" page.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ServerInfoPageAdminResource {
    
    private static final Logger logger = Logger.getLogger(ServerInfoPageAdminResource.class);

    @Context
    private KeycloakSession session;

    /**
     * Returns a list of providers and other operational info about the page.
     *
     * @return
     */
    @GET
    public ServerInfoRepresentation getInfo() {
        ServerInfoRepresentation info = new ServerInfoRepresentation();
        info.version = Version.VERSION;
        info.serverTime = new Date().toString();
        info.serverStartupTime = session.getKeycloakSessionFactory().getServerStartupTimestamp();
        info.memoryInfo = (new MemoryInfo()).init(Runtime.getRuntime());
        info.systemInfo = (new SystemInfo()).init();
        setProviders(info);
        return info;
    }

    private void setProviders(ServerInfoRepresentation info) {
        List<SpiInfoRepresentation> providers = new LinkedList<>();
        for (Spi spi : ServiceLoader.load(Spi.class)) {
            SpiInfoRepresentation spiRep = new SpiInfoRepresentation();
            spiRep.setName(spi.getName());
            spiRep.setInternal(spi.isInternal());
            spiRep.setSystemInfo(ServerInfoAwareProviderFactory.class.isAssignableFrom(spi.getProviderFactoryClass()));
            Set<String> s = session.listProviderIds(spi.getProviderClass());
            Set<SpiImplementationRepresentation> srs = new HashSet<>();
            
            if(s!=null){
                for(String name: s){
                    SpiImplementationRepresentation sr = new SpiImplementationRepresentation(name);
                    if(spiRep.isSystemInfo()){
                        sr.setOperationalInfo(((ServerInfoAwareProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(spi.getProviderClass(), name)).getOperationalInfo());
                    }
                    srs.add(sr);
                }
            }
            spiRep.setImplementations(srs);
            providers.add(spiRep);
        }
        info.providers = providers;
    }

    
    public static class MemoryInfo implements Serializable {
        
        protected long total;
        protected long used;
        
        public MemoryInfo(){
        }
        
        /**
         * Fill object fwith info.
         * @param runtime used to get memory info from.
         * @return itself for chaining
         */
        public MemoryInfo init(Runtime runtime){
            total = runtime.maxMemory();
            used = runtime.totalMemory() - runtime.freeMemory();
            return this;
        }
        
        public long getTotal(){
            return total;
        }
        
        public String getTotalFormated(){
            return formatMemory(getTotal());
        }
        
        public long getFree(){
            return getTotal() - getUsed();
        }

        public String getFreeFormated(){
            return formatMemory(getFree());
        }

        public long getUsed(){
            return used;
        }
        
        public String getUsedFormated(){
            return formatMemory(getUsed());
        }
        
        public long getFreePercentage(){
            return getFree() * 100 / getTotal();
        }
        
        private String formatMemory(long bytes){
            if(bytes > 1024L*1024L){
                return bytes/(1024L *1024L) + " MB"; 
            } else if(bytes > 1024L){
                return bytes/(1024L) + " kB"; 
            } else {
                return bytes + " B";
            }
        }
        
    }

    public static class SystemInfo implements Serializable {
        
        protected String javaVersion; 
        protected String javaVendor;
        protected String javaVm;
        protected String javaVmVersion;
        protected String javaRuntime;
        protected String javaHome;
        protected String osName;
        protected String osArchitecture;
        protected String osVersion;
        protected String fileEncoding;
        protected String userName;
        protected String userDir;
        protected String userTimezone;
        protected String userLocale;
        
        public SystemInfo() {
        }
        
        /**
         * Fill object with info about current system loaded from {@link System} properties.
         * @return object itself for chaining
         */
        protected SystemInfo init(){
            javaVersion = System.getProperty("java.version");
            javaVendor = System.getProperty("java.vendor");
            javaVm = System.getProperty("java.vm.name");
            javaVmVersion = System.getProperty("java.vm.version");
            javaRuntime = System.getProperty("java.runtime.name");
            javaHome = System.getProperty("java.home");
            osName = System.getProperty("os.name");
            osArchitecture = System.getProperty("os.arch");
            osVersion = System.getProperty("os.version");
            fileEncoding = System.getProperty("file.encoding");
            userName = System.getProperty("user.name");
            userDir = System.getProperty("user.dir");
            userTimezone = System.getProperty("user.timezone");
            userLocale = (new Locale(System.getProperty("user.country"),System.getProperty("user.language")).toString());
            return this;
        }
        
        public String getJavaVersion(){
            return javaVersion;
        }
        
        public String getJavaVendor(){
            return javaVendor;
        }
        
        public String getJavaVm(){
            return javaVm;
        }
        
        public String getJavaVmVersion(){
            return javaVmVersion;
        }

        public String getJavaRuntime(){
            return javaRuntime;
        }

        public String getJavaHome(){
            return javaHome;
        }
        
        public String getOsName(){
            return osName;
        }
        
        public String getOsArchitecture(){
            return osArchitecture;
        }
        
        public String getOsVersion(){
            return osVersion;
        }

        public String getFileEncoding(){
            return fileEncoding;
        }
        
        public String getUserName(){
            return userName;
        }
        
        public String getUserDir(){
            return userDir;
        }
        
        public String getUserTimezone(){
            return userTimezone;
        }
        
        public String getUserLocale(){
            return userLocale;
        }
    }
    
    public static class ServerInfoRepresentation implements Serializable {

        private String version;
        private String serverTime;
        private long serverStartupTime;


        private List<SpiInfoRepresentation> providers;
        
        private MemoryInfo memoryInfo;
        private SystemInfo systemInfo;
        
        public ServerInfoRepresentation() {
        }
        
        public SystemInfo getSystemInfo(){
            return systemInfo;
        }
        
        public MemoryInfo getMemoryInfo(){
            return memoryInfo;
        }

        public String getServerTime() {
            return serverTime;
        }
        
        public long getServerStartupTime() {
            return serverStartupTime;
        }
        
        /**
         * @return server startup time formatted
         */
        public String getServerStartupTimeFormatted() {
            return (new Date(serverStartupTime)).toString();
        }
        
        /**
         * @return server uptime in millis
         */
        public long getServerUptimeMillis(){
            return System.currentTimeMillis() - serverStartupTime;
        }
        
        /**
         * @return server uptime formatted like "0 days, 10 hours, 24 minutes, 55 seconds"
         */
        public String getServerUptime(){
            long diffInSeconds = getServerUptimeMillis()/1000;
            long diff[] = new long[] { 0, 0, 0, 0 };
            /* sec */diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
            /* min */diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
            /* hours */diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
            /* days */diff[0] = (diffInSeconds = (diffInSeconds / 24));

            return String.format(
                "%d day%s, %d hour%s, %d minute%s, %d second%s",
                diff[0],
                diff[0] != 1 ? "s" : "",
                diff[1],
                diff[1] != 1 ? "s" : "",
                diff[2],
                diff[2] != 1 ? "s" : "",
                diff[3],
                diff[3] != 1 ? "s" : "");
        }

        public String getVersion() {
            return version;
        }

        
        public List<SpiInfoRepresentation> getProviders() {
            return providers;
        }
    }

    public static class SpiInfoRepresentation implements Serializable {
        private String name;
        private boolean internal;
        private boolean systemInfo;
        private Set<SpiImplementationRepresentation> implementations;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isInternal() {
            return internal;
        }

        public void setInternal(boolean internal) {
            this.internal = internal;
        }

        public Set<SpiImplementationRepresentation> getImplementations() {
            return implementations;
        }

        public boolean isSystemInfo() {
            return systemInfo;
        }

        public void setSystemInfo(boolean systemInfo) {
            this.systemInfo = systemInfo;
        }

        public void setImplementations(Set<SpiImplementationRepresentation> implementations) {
            this.implementations = implementations;
        }
    }
    
    public static class SpiImplementationRepresentation implements Serializable {
        
        private String name;
        private Map<String, String> operationalInfo;
        
        public SpiImplementationRepresentation(String name) {
            super();
            this.name = name;
        }

        public Map<String, String> getOperationalInfo() {
            return operationalInfo;
        }

        public void setOperationalInfo(Map<String, String> operationalInfo) {
            this.operationalInfo = operationalInfo;
        }

        public String getName() {
            return name;
        }
        
    }
}
