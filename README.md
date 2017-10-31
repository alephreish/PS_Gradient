## Installation

To install the latest release: download the pre-compiled jar file [here](https://github.com/har-wradim/PS_Gradient/releases) and either do `Plugins → Install Plugin...` and choose the jar there or copy it to the plugins folder (e.g. `/opt/Fiji.app/plugins/`).

To compile and install the master branch:

	name=PS_Gradient
	imagej=/opt/Fiji.app # or other relevant location

	git clone "https://github.com/har-wradim/$name.git" && \
		javac *.java -cp "$imagej"/jars/*:"$imagej"/plugins/*:. -Xlint:unchecked && \
		jar -cf "$name.jar" *.class plugins.config && \
		mv "$name.jar" "$imagej/plugins/"

Either way, the plugin is than available under `Plugins → Analyze → PS Gradient`
