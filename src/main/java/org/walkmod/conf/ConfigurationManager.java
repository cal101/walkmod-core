/* 
  Copyright (C) 2013 Raquel Pau and Albert Coroleu.
 
 Walkmod is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Walkmod is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public License
 along with Walkmod.  If not, see <http://www.gnu.org/licenses/>.*/
package org.walkmod.conf;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.walkmod.ConfigurationAdapter;
import org.walkmod.conf.entities.Configuration;
import org.walkmod.conf.entities.impl.ConfigurationImpl;
import org.walkmod.conf.providers.LanguageConfigurationProvider;
import org.walkmod.conf.providers.PluginsConfigurationProvider;
import org.walkmod.conf.providers.SpringConfigurationProvider;
import org.walkmod.conf.providers.WritersConfigurationProvider;
import org.walkmod.conf.providers.XMLConfigurationProvider;
import org.walkmod.conf.providers.YAMLConfigurationProvider;
import org.walkmod.impl.DefaultConfigurationAdapter;

public class ConfigurationManager {

    private Configuration configuration;

    private List<ConfigurationProvider> configurationProviders = new LinkedList<ConfigurationProvider>();

    public ConfigurationManager(Configuration conf) {
        setConfiguration(conf);
    }

    public ConfigurationManager(Configuration conf, boolean execute, ConfigurationProvider... configurationProviders) {
        setConfiguration(conf);
        addProviders(execute, configurationProviders);
    }

    private void addProviders(boolean execute, ConfigurationProvider... configurationProviders) {
        this.configurationProviders.add(new PluginsConfigurationProvider());
        if (configurationProviders != null) {
            for (ConfigurationProvider cp : configurationProviders) {
                this.configurationProviders.add(cp);
            }
        }
        this.configurationProviders.add(new LanguageConfigurationProvider());
        this.configurationProviders.add(new WritersConfigurationProvider());
        // the class loader can be modified before
        this.configurationProviders.add(new SpringConfigurationProvider());
        if (execute) {
            executeConfigurationProviders();
            ConfigurationAdapter ca = new DefaultConfigurationAdapter();
            ca.setConfiguration(configuration);
            ca.prepare();
        }
    }

    public ConfigurationManager(File walkmodcfg, boolean execute, ConfigurationProvider... configurationProviders) {
        setConfiguration(new ConfigurationImpl());
        if (walkmodcfg.getName().endsWith(".xml")) {
            this.configurationProviders.add(new XMLConfigurationProvider(walkmodcfg.getAbsolutePath(), false));
        } else {
            this.configurationProviders.add(new YAMLConfigurationProvider(walkmodcfg.getAbsolutePath()));
        }
        addProviders(execute, configurationProviders);
    }

    public ConfigurationManager(File walkmodcfg, ConfigurationProvider... configurationProviders) {
        this(walkmodcfg, true, configurationProviders);
    }

    public ConfigurationManager(Configuration walkmodcfg, ConfigurationProvider... configurationProviders) {
        this(walkmodcfg, true, configurationProviders);
    }

    public ConfigurationManager(ConfigurationProvider... configurationProviders) {
        this(new File("walkmod.xml"), configurationProviders);
    }

    public ProjectConfigurationProvider getProjectConfigurationProvider() {
        Iterator<ConfigurationProvider> it = configurationProviders.iterator();
        while (it.hasNext()) {
            ConfigurationProvider current = it.next();
            if (current instanceof ProjectConfigurationProvider) {
                return (ProjectConfigurationProvider) current;
            }
        }
        return null;
    }

    public void runProjectConfigurationInitializers() throws Exception {
        ProjectConfigurationProvider cfgProvider = getProjectConfigurationProvider();
        if (cfgProvider != null) {
            cfgProvider.createConfig();

            executeConfigurationProviders();
            configuration.runInitializers(cfgProvider);
        }
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void executeConfigurationProviders() {
        Iterator<ConfigurationProvider> it = configurationProviders.iterator();
        while (it.hasNext()) {
            ConfigurationProvider current = it.next();
            current.init(getConfiguration());
            current.load();
        }
    }

    /**
     * Get the current list of ConfigurationProviders. If no custom ConfigurationProviders have been
     * added, this method will return a list containing only the default ConfigurationProvider,
     * XMLConfigurationProvider. if a custom ConfigurationProvider has been added, then the
     * XmlConfigurationProvider must be added by hand.
     * 
     * @return the list of registered ConfigurationProvider objects
     * @see ConfigurationProvider
     * 
     */
    public List<ConfigurationProvider> getConfigurationProviders() {
        return configurationProviders;
    }

    /**
     * Set the list of configuration providers
     *
     * @param configurationProviders
     *            the ConfigurationProvider to register
     */
    public void setConfigurationProviders(List<ConfigurationProvider> configurationProviders) {
        this.configurationProviders = configurationProviders;
    }

    /**
     * Adds a configuration provider to the List of ConfigurationProviders. a given
     * ConfigurationProvider may be added more than once
     *
     * @param provider
     *            the ConfigurationProvider to register
     */
    public void addConfigurationProvider(ConfigurationProvider provider) {
        if (!configurationProviders.contains(provider)) {
            configurationProviders.add(provider);
        }
    }

    public void clearConfigurationProviders() {
        configurationProviders.clear();
    }

    /**
     * Destroy its managing Configuration instance
     */
    public void destroyConfiguration() {
        configuration = null;
    }
}
