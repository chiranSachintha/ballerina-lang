import lang.annotations.doc1 as doc;

@doc:Description{queryParamValue: [@doc:QueryParam{
                    name:"paramName", 
                    value:"paramValue"}, "hello"]
                }
function foo (string args) {
    // do nothing
}
