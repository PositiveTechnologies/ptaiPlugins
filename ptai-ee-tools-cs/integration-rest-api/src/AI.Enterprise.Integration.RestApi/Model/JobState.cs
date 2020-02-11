/* 
 * Api Documentation
 *
 * Api Documentation
 *
 * The version of the OpenAPI document: 1.0
 * 
 * Generated by: https://github.com/openapitools/openapi-generator.git
 */

using System;
using System.Linq;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Runtime.Serialization;
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;
using System.ComponentModel.DataAnnotations;
using OpenAPIDateConverter = AI.Enterprise.Integration.RestApi.Client.OpenAPIDateConverter;

namespace AI.Enterprise.Integration.RestApi.Model
{
    /// <summary>
    /// JobState
    /// </summary>
    [DataContract]
    public partial class JobState :  IEquatable<JobState>, IValidatableObject
    {
        /// <summary>
        /// Defines Status
        /// </summary>
        [JsonConverter(typeof(StringEnumConverter))]
        public enum StatusEnum
        {
            /// <summary>
            /// Enum UNKNOWN for value: UNKNOWN
            /// </summary>
            [EnumMember(Value = "UNKNOWN")]
            UNKNOWN = 1,

            /// <summary>
            /// Enum FAILURE for value: FAILURE
            /// </summary>
            [EnumMember(Value = "FAILURE")]
            FAILURE = 2,

            /// <summary>
            /// Enum UNSTABLE for value: UNSTABLE
            /// </summary>
            [EnumMember(Value = "UNSTABLE")]
            UNSTABLE = 3,

            /// <summary>
            /// Enum SUCCESS for value: SUCCESS
            /// </summary>
            [EnumMember(Value = "SUCCESS")]
            SUCCESS = 4,

            /// <summary>
            /// Enum ABORTED for value: ABORTED
            /// </summary>
            [EnumMember(Value = "ABORTED")]
            ABORTED = 5

        }

        /// <summary>
        /// Gets or Sets Status
        /// </summary>
        [DataMember(Name="status", EmitDefaultValue=false)]
        public StatusEnum? Status { get; set; }
        /// <summary>
        /// Initializes a new instance of the <see cref="JobState" /> class.
        /// </summary>
        /// <param name="log">log.</param>
        /// <param name="pos">pos.</param>
        /// <param name="status">status.</param>
        public JobState(string log = default(string), int pos = default(int), StatusEnum? status = default(StatusEnum?))
        {
            this.Log = log;
            this.Pos = pos;
            this.Status = status;
        }
        
        /// <summary>
        /// Gets or Sets Log
        /// </summary>
        [DataMember(Name="log", EmitDefaultValue=false)]
        public string Log { get; set; }

        /// <summary>
        /// Gets or Sets Pos
        /// </summary>
        [DataMember(Name="pos", EmitDefaultValue=false)]
        public int Pos { get; set; }


        /// <summary>
        /// Returns the string presentation of the object
        /// </summary>
        /// <returns>String presentation of the object</returns>
        public override string ToString()
        {
            var sb = new StringBuilder();
            sb.Append("class JobState {\n");
            sb.Append("  Log: ").Append(Log).Append("\n");
            sb.Append("  Pos: ").Append(Pos).Append("\n");
            sb.Append("  Status: ").Append(Status).Append("\n");
            sb.Append("}\n");
            return sb.ToString();
        }
  
        /// <summary>
        /// Returns the JSON string presentation of the object
        /// </summary>
        /// <returns>JSON string presentation of the object</returns>
        public virtual string ToJson()
        {
            return JsonConvert.SerializeObject(this, Formatting.Indented);
        }

        /// <summary>
        /// Returns true if objects are equal
        /// </summary>
        /// <param name="input">Object to be compared</param>
        /// <returns>Boolean</returns>
        public override bool Equals(object input)
        {
            return this.Equals(input as JobState);
        }

        /// <summary>
        /// Returns true if JobState instances are equal
        /// </summary>
        /// <param name="input">Instance of JobState to be compared</param>
        /// <returns>Boolean</returns>
        public bool Equals(JobState input)
        {
            if (input == null)
                return false;

            return 
                (
                    this.Log == input.Log ||
                    (this.Log != null &&
                    this.Log.Equals(input.Log))
                ) && 
                (
                    this.Pos == input.Pos ||
                    (this.Pos != null &&
                    this.Pos.Equals(input.Pos))
                ) && 
                (
                    this.Status == input.Status ||
                    (this.Status != null &&
                    this.Status.Equals(input.Status))
                );
        }

        /// <summary>
        /// Gets the hash code
        /// </summary>
        /// <returns>Hash code</returns>
        public override int GetHashCode()
        {
            unchecked // Overflow is fine, just wrap
            {
                int hashCode = 41;
                if (this.Log != null)
                    hashCode = hashCode * 59 + this.Log.GetHashCode();
                if (this.Pos != null)
                    hashCode = hashCode * 59 + this.Pos.GetHashCode();
                if (this.Status != null)
                    hashCode = hashCode * 59 + this.Status.GetHashCode();
                return hashCode;
            }
        }

        /// <summary>
        /// To validate all properties of the instance
        /// </summary>
        /// <param name="validationContext">Validation context</param>
        /// <returns>Validation Result</returns>
        IEnumerable<System.ComponentModel.DataAnnotations.ValidationResult> IValidatableObject.Validate(ValidationContext validationContext)
        {
            yield break;
        }
    }

}
