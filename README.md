## Installation

Either install the latest release: download the pre-compiled jar file [here](https://github.com/har-wradim/PS_Gradient/releases) and copy it to the plugins folder of ImageJ/Fiji (e.g. `/opt/Fiji.app/plugins/`).

To compile yourself: clone the repository (`git clone https://github.com/har-wradim/PS_Gradient.git`) or download it as [zip](https://github.com/har-wradim/PS_Gradient/archive/master.zip) and compile:

	name=PS_Gradient
	imagej=/opt/Fiji.app # or other relevant location
	javac "$name.java" -cp "$imagej"/jars/*:"$imagej"/plugins/*:. -Xlint:unchecked && \
		jar -cf "$name.jar" "$name.class" plugins.config && \
		mv "$name.jar" "$imagej/plugins/"

The plugin is than available under Plugins → Analyze → PS Gradient
