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
package org.walkmod.conf.providers;

import java.net.URL;
import java.util.Collection;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.walkmod.conf.BeanFactoryProvider;
import org.walkmod.conf.ConfigurationException;
import org.walkmod.conf.ConfigurationProvider;
import org.walkmod.conf.entities.Configuration;
import org.walkmod.conf.entities.PluginConfig;

public class SpringConfigurationProvider implements ConfigurationProvider, BeanFactoryProvider {

	private Configuration configuration;

	private String config;

	public SpringConfigurationProvider() {
		this("application-context.xml");
	}

	public SpringConfigurationProvider(String config) {
		this.config = config;
	}

	@Override
	public void init(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void loadBeanFactory() throws ConfigurationException {

		GenericApplicationContext ctx = new GenericApplicationContext();
		ClassLoader currentClassLoader = configuration.getClassLoader();
		if (currentClassLoader != Thread.currentThread().getContextClassLoader()) {
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
			reader.setBeanClassLoader(currentClassLoader);
			reader.loadBeanDefinitions(new ClassPathResource(config, currentClassLoader));
			Collection<PluginConfig> plugins = configuration.getPlugins();
			if (plugins != null) {
				for (PluginConfig plugin : plugins) {
					String descriptorName = plugin.getArtifactId();
					if (!descriptorName.startsWith("walkmod-")) {
						descriptorName = "walkmod-" + descriptorName;
					}
					if (!descriptorName.endsWith("-plugin")) {
						descriptorName = descriptorName + "-plugin";
					}

					reader.loadBeanDefinitions(new ClassPathResource("META-INF/walkmod/" + descriptorName + ".xml",
							configuration.getClassLoader()));

					URL url = currentClassLoader.getResource("META-INF/walkmod2/" + descriptorName + ".xml");
					if (url != null) {
						reader.loadBeanDefinitions(new ClassPathResource("META-INF/walkmod2/" + descriptorName + ".xml",
								configuration.getClassLoader()));
					}
				}
			}
			configuration.setBeanDefinitionRegistry(reader.getRegistry());
			ctx.refresh();
		}

		configuration.setBeanFactory(ctx);
	}

	@Override
	public void load() throws ConfigurationException {
		loadBeanFactory();
	}
}
