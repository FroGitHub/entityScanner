<script type="module">

import mermaid from "./mermaid.esm.min.mjs"

mermaid.initialize({
    startOnLoad:false
})

window.renderDiagram = async function(code){

    document.getElementById("graph").innerHTML =
        `<pre class="mermaid">${code}</pre>`

    await mermaid.run()
}

</script>
