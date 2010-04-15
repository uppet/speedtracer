/** 
 * JSONSchema Validator - Validates JavaScript objects using JSON Schemas 
 *  (http://www.json.com/json-schema-proposal/)
 *
 * Copyright (c) 2007 Kris Zyp SitePen (www.sitepen.com)
 * Licensed under the MIT (MIT-LICENSE.txt) license.
To use the validator call JSONSchema.validate with an instance object and an optional schema object.
If a schema is provided, it will be used to validate. If the instance object refers to a schema (self-validating), 
that schema will be used to validate and the schema parameter is not necessary (if both exist, 
both validations will occur). 
The validate method will return an array of validation errors. If there are no errors, then an 
empty list will be returned. A validation error will have two properties: 
"property" which indicates which property had the error
"message" which indicates what the error was
 */

/*
 * Modified by Chris Conroy <conroy@google.com>
 *  -modified issue 17's proposed patch (http://code.google.com/p/jsonschema/issues/detail?id=17)
 *  -added logic to walk up the inheritance tree to gather parent properties
 *  when an object extends another schema
 *  -fixed type checking of Arrays created outside the context by using Douglas
 *  Crockford's method.
 */

JSONSchema = {
        /*
         * A user provided function that can resolve a reference.
         *
         * Example to resolve from a user maintained dictionary
         *
         * function resolveSchema(reference) {
         *   return schemas[reference];
         * }
         */

        //Default impl errors on any ref.
        resolveReference: function(/*Object*/ reference) {
                            return null;
        },
  validate : function(/*Any*/instance,/*Object*/schema) {
    // Summary:
    //    To use the validator call JSONSchema.validate with an instance object and an optional schema object.
    //     If a schema is provided, it will be used to validate. If the instance object refers to a schema (self-validating), 
    //     that schema will be used to validate and the schema parameter is not necessary (if both exist, 
    //     both validations will occur). 
    //     The validate method will return an object with two properties:
    //       valid: A boolean indicating if the instance is valid by the schema
    //       errors: An array of validation errors. If there are no errors, then an 
    //           empty list will be returned. A validation error will have two properties: 
    //             property: which indicates which property had the error
    //             message: which indicates what the error was
    //
    return this._validate(instance,schema,false);
  },
  checkPropertyChange : function(/*Any*/value,/*Object*/schema, /*String*/ property) {
    // Summary:
    //     The checkPropertyChange method will check to see if an value can legally be in property with the given schema
    //     This is slightly different than the validate method in that it will fail if the schema is readonly and it will
    //     not check for self-validation, it is assumed that the passed in value is already internally valid.  
    //     The checkPropertyChange method will return the same object type as validate, see JSONSchema.validate for 
    //     information.
    //
    return this._validate(value,schema, property || "property");
  },
  _validate : function(/*Any*/instance,/*Object*/schema,/*Boolean*/ _changing) {
  
  var errors = [];
  // validate a value against a property definition
                
  // Internal wrapper for the user-provided resolve method
  function _resolve(/*Object*/ schema) {
    if(schema && schema.$ref){
      var resolvedSchema = JSONSchema.resolveReference(schema.$ref);
      if(!resolvedSchema){
        addError("Unable to resolve schema reference to '" + schema.$ref + "'");
        return schema;
      } else {
        return resolvedSchema;
      }
    } else {
      return schema;
    }
  }

  /* Walk up the inheritance tree to gather all properties
   * TODO: multiple inheritance
   */
  function _getAllProps(/*Object*/ schema) {
    var ret = {};
    if(schema['extends']) {
      ret = _getAllProps(_resolve(schema['extends']));
    }
    for(prop in schema.properties) {
      if(schema.properties.hasOwnProperty(prop)) {
        ret[prop] = schema.properties[prop];
      }
    }
    return ret;
  }

  /**
   * Douglas Crockford's method for instanceOf checking for arrays
   * created outside of the context. (Modified for only Array, not generic
   * type checking)
   * http://javascript.crockford.com/remedial.html
   */
  function instanceOfArray(value) {
    //If instanceOf returns True, we can stop there
    if (value instanceof Array) {
      return true;
    }
    if (typeof value === 'object') {
      if (value) {
        if (typeof value.length === 'number' &&
            !(value.propertyIsEnumerable('length')) &&
            typeof value.splice === 'function') {
          return true;
        }
      }
    }
    return false;
  }

  function checkProp(value, schema, path,i){
    var l;
    path += path ? typeof i == 'number' ? '[' + i + ']' : typeof i == 'undefined' ? '' : '.' + i : i;
    function addError(message){
      errors.push({property:path,message:message});
    }

    /* If the schema is really a reference, then hotswap to its
     * definition */
    schema = _resolve(schema);
    
    if((typeof schema != 'object' || instanceOfArray(schema)) && (path || typeof schema != 'function')){
      if(typeof schema == 'function'){
        if(!(value instanceof schema)){
          addError("is not an instance of the class/constructor " + schema.name);
        }
      }else if(schema){
        addError("Invalid schema/property definition " + schema);
      }
      return null;
    }
    if(_changing && schema.readonly){
      addError("is a readonly field, it can not be changed");
    }
    if(schema['extends']){ // if it extends another schema, it must pass that schema as well
      checkProp(value,schema['extends'],path,i);
    }
    // validate a value against a type definition
    function checkType(type,value){
      if(type){
        if(typeof type == 'string' && type != 'any' && 
            (type == 'null' ? value !== null : typeof value != type) && 
            !(type == 'array' && instanceOfArray(value)) &&
            !(type == 'integer' && value%1===0)){
          return [{property:path,message:(typeof value) + " value found, but a " + type + " is required"}];
        }
        if(type instanceof Array){
          var unionErrors=[];
          for(var j = 0; j < type.length; j++){ // a union type 
            if(!(unionErrors=checkType(type[j],value)).length){
              break;
            }
          }
          if(unionErrors.length){
            return unionErrors;
          }
        }else if(typeof type == 'object'){
          var priorErrors = errors;
          errors = []; 
          checkProp(value,type,path);
          var theseErrors = errors;
          errors = priorErrors;
          return theseErrors; 
        } 
      }
      return [];
    }
    if(value === undefined){
      if(!schema.optional){  
        addError("is missing and it is not optional");
      }
    }else{
      errors = errors.concat(checkType(schema.type,value));
      if(schema.disallow && !checkType(schema.disallow,value).length){
        addError(" disallowed value was matched");
      }
      if(value !== null){
        if(instanceOfArray(value)){
          if(schema.items){
            if(instanceOfArray(schema.items)){
              for(i=0,l=value.length; i<l; i++){
                errors.concat(checkProp(value[i],schema.items[i],path,i));
              }
            }else{
              for(i=0,l=value.length; i<l; i++){
                errors.concat(checkProp(value[i],schema.items,path,i));
              }
            }              
          }
          if(schema.minItems && value.length < schema.minItems){
            addError("There must be a minimum of " + schema.minItems + " in the array");
          }
          if(schema.maxItems && value.length > schema.maxItems){
            addError("There must be a maximum of " + schema.maxItems + " in the array");
          }
        }else if(schema.properties){
                                        var fullProps = _getAllProps(schema);
          errors.concat(checkObj(value,fullProps,path,schema.additionalProperties));
        }
        if(schema.pattern && typeof value == 'string' && !value.match(schema.pattern)){
          addError("does not match the regex pattern " + schema.pattern);
        }
        if(schema.maxLength && typeof value == 'string' && value.length > schema.maxLength){
          addError("may only be " + schema.maxLength + " characters long");
        }
        if(schema.minLength && typeof value == 'string' && value.length < schema.minLength){
          addError("must be at least " + schema.minLength + " characters long");
        }
        if(typeof schema.minimum !== undefined && typeof value == typeof schema.minimum && 
            schema.minimum > value){
          addError("must have a minimum value of " + schema.minimum);
        }
        if(typeof schema.maximum !== undefined && typeof value == typeof schema.maximum && 
            schema.maximum < value){
          addError("must have a maximum value of " + schema.maximum);
        }
        if(schema['enum']){
          var enumer = schema['enum'];
          l = enumer.length;
          var found;
          for(var j = 0; j < l; j++){
            if(enumer[j]===value){
              found=1;
              break;
            }
          }
          if(!found){
            addError("value [" + value + "] is not in the enumeration [" + enumer.join(", ") + "]");
          }
        }
        if(typeof schema.maxDecimal == 'number' && 
          (value.toString().match(new RegExp("\\.[0-9]{" + (schema.maxDecimal + 1) + ",}")))){
          addError("may only have " + schema.maxDecimal + " digits of decimal places");
        }
      }
    }
    return null;
  }
  // validate an object against a schema
  function checkObj(instance,objTypeDef,path,additionalProp){
  
    if(typeof objTypeDef =='object'){
      if(typeof instance != 'object' || instanceOfArray(instance)){
        errors.push({property:path,message:"an object is required"});
      }
      
      for(var i in objTypeDef){ 
        if(objTypeDef.hasOwnProperty(i) && !(i.charAt(0) == '_' && i.charAt(1) == '_')){
          var value = instance[i];
          var propDef = objTypeDef[i];
          checkProp(value,propDef,path,i);
        }
      }
    }
    for(i in instance){
      if(instance.hasOwnProperty(i) && !(i.charAt(0) == '_' && i.charAt(1) == '_') && objTypeDef && !objTypeDef[i] && additionalProp===false){
        errors.push({property:path,message:(typeof value) + "The property " + i +
            " is not defined in the schema and the schema does not allow additional properties"});
      }
      var requires = objTypeDef && objTypeDef[i] && objTypeDef[i].requires;
      if(requires && !(requires in instance)){
        errors.push({property:path,message:"the presence of the property " + i + " requires that " + requires + " also be present"});
      }
      value = instance[i];
      if(objTypeDef && typeof objTypeDef == 'object' && !(i in objTypeDef)){
        checkProp(value,additionalProp,path,i); 
      }
      if(!_changing && value && value.$schema){
        errors = errors.concat(checkProp(value,value.$schema,path,i));
      }
    }
    return errors;
  }
  if(schema){
    checkProp(instance,schema,'',_changing || '');
  }
  if(!_changing && instance && instance.$schema){
    checkProp(instance,instance.$schema,'','');
  }
  return {valid:!errors.length,errors:errors};
  },
  /* will add this later
  newFromSchema : function() {
  }
*/
}
