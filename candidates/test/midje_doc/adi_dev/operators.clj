
[[:section {:title "adi.core"}]]


[[:subsection {:title "connect-env!"}]]

(connect-env!
  "datomic:mem://adi-test-api-linked" schema :install :reset)

[[:subsection {:title "release-env!"}]]

(release-env!
  "datomic:mem://adi-test-api-linked" schema :delete)


[[:subsection {:title "install-schema!"}]]
(install-schema! env)

[[:subsection {:title "query"}]]

""
(query env '[:find ?x :where 
               [?x :account/user "Chris"]]
       :access {:account/user :checked})

[[:subsection {:title "query-ids"}]]

""

(query-ids env '[:find ?x :where 
            [?x :account/user "Chris"]])


[[:subsection {:title "query-entities"}]]

[[:subsection {:title "select"}]]

[[:subsection {:title "select-ids"}]]

[[:subsection {:title "select-entities"}]]

[[:subsection {:title "insert!"}]]

[[:subsection {:title "update!"}]]

[[:subsection {:title "update-in!"}]]

[[:subsection {:title "delete!"}]]

[[:subsection {:title "delete-in!"}]]

[[:subsection {:title "delete-all!"}]]

[[:subsection {:title "retract!"}]]

[[:subsection {:title "retract-in!"}]]

[[:subsection {:title "->sync"}]]


[[:section {:title "adi.util"}]]

[[:subsection {:title "transactions"}]]

[[:subsection {:title "schema"}]]

[[:chapter {:title "Options"}]]

[[:section {:title "Bans"}]]

[[:subsection {:title ":ban-top-id"}]]
[[:subsection {:title ":ban-id"}]]
[[:subsection {:title ":ban-expressions"}]]

[[:section {:title "Schema"}]]

[[:subsection {:title ":schema-required"}]]
[[:subsection {:title ":schema-restrict"}]]
[[:subsection {:title ":schema-defaults"}]]
[[:subsection {:title ":schema-raise"}]]

[[:section {:title "Model"}]]

[[:subsection {:title ":use-typecheck"}]]
[[:subsection {:title ":use-coerce"}]]

[[:section {:title "Skip"}]]

[[:subsection {:title ":skip-normalise"}]]
[[:subsection {:title ":skip-typesafety"}]]

[[:section {:title "Select"}]]

[[:subsection {:title ":first"}]]
[[:subsection {:title ":ids"}]]

[[:section {:title "Testing"}]]

[[:subsection {:title ":generate-ids"}]]
[[:subsection {:title ":generate-syms"}]]
[[:subsection {:title ":raw"}]]

[[:chapter {:title "Profiles"}]]
[[:section {:title ":select"}]]
[[:section {:title ":modify"}]]
[[:section {:title ":modify-select"}]]
[[:section {:title ":insert"}]]
[[:section {:title ":delete"}]]

[[:chapter {:title "Environment"}]]

[[:section {:title ":url"}]]

"This is the url of the env. It is used to reconnect to the datomic transactor is connection is lost"

[[:section {:title ":conn"}]]

"This holds the connection object that is used to communicate with the the transactor"

[[:section {:title ":db"}]]

"This holds the database value."

[[:section {:title ":at"}]]

"This holds the time or transaction number of the of the database"

[[:section {:title ":op"}]]

"This holds information about the type of operation that is being performed"

(comment
  :select :insert :delete :delete-in :retract :retract-in :update-in :update)

[[:section {:title ":schema"}]]

"This holds the schema. it should be an executable schema or xm with :tree, :flat and :lookup entries"

[[:section {:title ":data"}]]

"This is the data that is currently being processed"

[[:section {:title ":profile"}]]

"This is the security profile that governs a particular interface"

[[:section {:title ":model"}]]

"This is the model used to transform and validate input data"

[[:section {:title ":access"}]]

"This is a model that can be used to specify input and pull conditions"

[[:section {:title ":transact"}]]

"This conveys how results should be pulled. The default is :async"

[[:chapter {:title "Models"}]]

[[:section {:title "Refine"}]]
[[:subsection {:title ":prepare"}]]

"Is a catch-all function that is run on the input data at the very beginning of refine"

[[:subsection {:title ":require"}]]

"Implements checks on the data fields to ensure that the data is actually there."

[[:subsection {:title ":fill"}]]

"If any of the data is empty, then fills out the data with value or a function taking"

(comment 
  (fn [refs env] ....))


[[:subsection {:title ":rewrite"}]]

"Overwrites the data only when the data is there."

(comment 
  (fn [refs env] ....))

[[:subsection {:title ":cull"}]]

"Filters out the data from the original data"

[[:subsection {:title ":ignore"}]]

"If the model is not in the schema, do not throw an error"

[[:subsection {:title ":allow"}]]

"This implements the allowed fields within a data model"

[[:subsection {:title ":validate"}]]

"This is the validation function for the model. takes the value and the env as arguments"

(comment 
  (fn [v env] ....))

[[:subsection {:title ":transform"}]]

"This is the transformation function for the model. takes the value and the env as arguments"

(comment 
  (fn [v env] ....))


[[:subsection {:title ":expression"}]]

"This is a filter for expressions"

[[:section {:title "Enhance"}]]
[[:subsection {:title ":generate"}]]

"This generates additional fields from the processed data"

"NOT IMPLEMENTED"

[[:subsection {:title ":mask"}]]

"This hides data"

"NOT IMPLEMENTED"


[[:section {:title "Unpack"}]]
[[:subsection {:title ":pull"}]]
"This specifies which data is to be pulled on the output model."

[[:subsection {:title ":censor"}]]
"NOT IMPLEMENTED"

[[:subsection {:title ":format"}]]
"NOT IMPLEMENTED"

