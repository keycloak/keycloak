package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.keycloak.Version;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.mongo.MongoConnectionProvider;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.exportimport.ClientImporter;
import org.keycloak.exportimport.ClientImporterFactory;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.provider.MonitorableProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderOperationalInfo;
import org.keycloak.provider.Spi;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperTypeRepresentation;
import org.keycloak.social.SocialIdentityProvider;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ServerInfoAdminResource {
    
    private static final Logger logger = Logger.getLogger(ServerInfoAdminResource.class);

    private static final Map<String, List<String>> ENUMS = createEnumsMap(EventType.class, OperationType.class);

    @Context
    private KeycloakSession session;

    /**
     * Returns a list of themes, social providers, auth providers, and event listeners available on this server
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
        setSocialProviders(info);
        setIdentityProviders(info);
        setThemes(info);
        setEventListeners(info);
        setProtocols(info);
        setClientImporters(info);
        setProviders(info);
        setProtocolMapperTypes(info);
        setBuiltinProtocolMappers(info);
        info.setEnums(ENUMS);
        
        ProviderFactory<JpaConnectionProvider> jpf = session.getKeycloakSessionFactory().getProviderFactory(JpaConnectionProvider.class);
        if(jpf!=null && jpf instanceof MonitorableProviderFactory){
           info.jpaInfo = ((MonitorableProviderFactory<?>)jpf).getOperationalInfo();
        } else {
            logger.debug("JPA provider not found or is not monitorable");
        }
        
        ProviderFactory<MongoConnectionProvider> mpf = session.getKeycloakSessionFactory().getProviderFactory(MongoConnectionProvider.class);
        if(mpf!=null && mpf instanceof MonitorableProviderFactory){
           info.mongoDbInfo = ((MonitorableProviderFactory<?>)mpf).getOperationalInfo();
        } else {
            logger.debug("Mongo provider not found or is not monitorable");
        }
                
        return info;
    }

    private void setProviders(ServerInfoRepresentation info) {
        List<SpiInfoRepresentation> providers = new LinkedList<>();
        for (Spi spi : ServiceLoader.load(Spi.class)) {
            SpiInfoRepresentation spiRep = new SpiInfoRepresentation();
            spiRep.setName(spi.getName());
            spiRep.setInternal(spi.isInternal());
            spiRep.setImplementations(session.listProviderIds(spi.getProviderClass()));
            providers.add(spiRep);
        }
        info.providers = providers;
    }

    private void setThemes(ServerInfoRepresentation info) {
        ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
        info.themes = new HashMap<String, List<String>>();

        for (Theme.Type type : Theme.Type.values()) {
            List<String> themes = new LinkedList<String>(themeProvider.nameSet(type));
            Collections.sort(themes);

            info.themes.put(type.toString().toLowerCase(), themes);
        }
    }

    private void setSocialProviders(ServerInfoRepresentation info) {
        info.socialProviders = new LinkedList<>();
        List<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class);
        setIdentityProviders(providerFactories, info.socialProviders, "Social");
    }

    private void setIdentityProviders(ServerInfoRepresentation info) {
        info.identityProviders = new LinkedList<>();
        List<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactories(IdentityProvider.class);
        setIdentityProviders(providerFactories, info.identityProviders, "User-defined");

        providerFactories = session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class);
        setIdentityProviders(providerFactories, info.identityProviders, "Social");
    }

    public void setIdentityProviders(List<ProviderFactory> factories, List<Map<String, String>> providers, String groupName) {
        for (ProviderFactory providerFactory : factories) {
            IdentityProviderFactory factory = (IdentityProviderFactory) providerFactory;
            Map<String, String> data = new HashMap<>();
            data.put("groupName", groupName);
            data.put("name", factory.getName());
            data.put("id", factory.getId());

            providers.add(data);
        }
    }

    private void setEventListeners(ServerInfoRepresentation info) {
        info.eventListeners = new LinkedList<String>();

        Set<String> providers = session.listProviderIds(EventListenerProvider.class);
        if (providers != null) {
            info.eventListeners.addAll(providers);
        }
    }

    private void setProtocols(ServerInfoRepresentation info) {
        info.protocols = new LinkedList<String>();
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(LoginProtocol.class)) {
            info.protocols.add(p.getId());
        }
        Collections.sort(info.protocols);
    }

    private void setProtocolMapperTypes(ServerInfoRepresentation info) {
        info.protocolMapperTypes = new HashMap<String, List<ProtocolMapperTypeRepresentation>>();
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(ProtocolMapper.class)) {
            ProtocolMapper mapper = (ProtocolMapper)p;
            List<ProtocolMapperTypeRepresentation> types = info.protocolMapperTypes.get(mapper.getProtocol());
            if (types == null) {
                types = new LinkedList<ProtocolMapperTypeRepresentation>();
                info.protocolMapperTypes.put(mapper.getProtocol(), types);
            }
            ProtocolMapperTypeRepresentation rep = new ProtocolMapperTypeRepresentation();
            rep.setId(mapper.getId());
            rep.setName(mapper.getDisplayType());
            rep.setHelpText(mapper.getHelpText());
            rep.setCategory(mapper.getDisplayCategory());
            rep.setProperties(new LinkedList<ConfigPropertyRepresentation>());
            List<ProviderConfigProperty> configProperties = mapper.getConfigProperties();
            for (ProviderConfigProperty prop : configProperties) {
                ConfigPropertyRepresentation propRep = new ConfigPropertyRepresentation();
                propRep.setName(prop.getName());
                propRep.setLabel(prop.getLabel());
                propRep.setType(prop.getType());
                propRep.setDefaultValue(prop.getDefaultValue());
                propRep.setHelpText(prop.getHelpText());
                rep.getProperties().add(propRep);
            }
            types.add(rep);
        }
    }

    private void setBuiltinProtocolMappers(ServerInfoRepresentation info) {
        info.builtinProtocolMappers = new HashMap<>();
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(LoginProtocol.class)) {
            LoginProtocolFactory factory = (LoginProtocolFactory)p;
            List<ProtocolMapperRepresentation> mappers = new LinkedList<>();
            for (ProtocolMapperModel mapper : factory.getBuiltinMappers()) {
                mappers.add(ModelToRepresentation.toRepresentation(mapper));
            }
            info.builtinProtocolMappers.put(p.getId(), mappers);
        }
    }

    private void setClientImporters(ServerInfoRepresentation info) {
        info.clientImporters = new LinkedList<Map<String, String>>();
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(ClientImporter.class)) {
            ClientImporterFactory factory = (ClientImporterFactory)p;
            Map<String, String> data = new HashMap<String, String>();
            data.put("id", factory.getId());
            data.put("name", factory.getDisplayName());
            info.clientImporters.add(data);
        }
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

        private Map<String, List<String>> themes;

        private List<Map<String, String>> socialProviders;
        public List<Map<String, String>> identityProviders;
        private List<String> protocols;
        private List<Map<String, String>> clientImporters;

        private List<SpiInfoRepresentation> providers;

        private List<String> eventListeners;
        private Map<String, List<ProtocolMapperTypeRepresentation>> protocolMapperTypes;
        private Map<String, List<ProtocolMapperRepresentation>> builtinProtocolMappers;

        private Map<String, List<String>> enums;
        
        private MemoryInfo memoryInfo;
        private SystemInfo systemInfo;
        
        private ProviderOperationalInfo jpaInfo;
        private ProviderOperationalInfo mongoDbInfo;

        public ServerInfoRepresentation() {
        }
        
        public SystemInfo getSystemInfo(){
            return systemInfo;
        }
        
        public MemoryInfo getMemoryInfo(){
            return memoryInfo;
        }

        public ProviderOperationalInfo getJpaInfo() {
            return jpaInfo;
        }
        
        public ProviderOperationalInfo getMongoDbInfo() {
            return mongoDbInfo;
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

        public Map<String, List<String>> getThemes() {
            return themes;
        }

        public List<Map<String, String>> getSocialProviders() {
            return socialProviders;
        }

        public List<Map<String, String>> getIdentityProviders() {
            return this.identityProviders;
        }

        public List<String> getEventListeners() {
            return eventListeners;
        }

        public List<String> getProtocols() {
            return protocols;
        }

        public List<Map<String, String>> getClientImporters() {
            return clientImporters;
        }

        public List<SpiInfoRepresentation> getProviders() {
            return providers;
        }

        public Map<String, List<ProtocolMapperTypeRepresentation>> getProtocolMapperTypes() {
            return protocolMapperTypes;
        }

        public Map<String, List<ProtocolMapperRepresentation>> getBuiltinProtocolMappers() {
            return builtinProtocolMappers;
        }

        public void setBuiltinProtocolMappers(Map<String, List<ProtocolMapperRepresentation>> builtinProtocolMappers) {
            this.builtinProtocolMappers = builtinProtocolMappers;
        }

        public Map<String, List<String>> getEnums() {
            return enums;
        }

        public void setEnums(Map<String, List<String>> enums) {
            this.enums = enums;
        }
    }

    public static class SpiInfoRepresentation implements Serializable {
        private String name;
        private boolean internal;
        private Set<String> implementations;

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

        public Set<String> getImplementations() {
            return implementations;
        }

        public void setImplementations(Set<String> implementations) {
            this.implementations = implementations;
        }
    }

    private static Map<String, List<String>> createEnumsMap(Class... enums) {
        Map<String, List<String>> m = new HashMap<>();
        for (Class e : enums) {
            String n = e.getSimpleName();
            n = Character.toLowerCase(n.charAt(0)) + n.substring(1);

            List<String> l = new LinkedList<>();
            for (Object c :  e.getEnumConstants()) {
                l.add(c.toString());
            }
            Collections.sort(l);

            m.put(n, l);
        }
        return m;
    }

}
