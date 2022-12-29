USE seichiassist;

CREATE TABLE IF NOT EXISTS donate_usage_history(
    id INT AUTO_INCREMENT NOT NULL,
    uuid CHAR(36) NOT NULL,
    effect_name VARCHAR(20) NOT NULL,
    use_points INT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY(id, uuid)
);

INSERT INTO donate_usage_history(
    uuid,
    effect_name,
    use_points,
    timestamp
)
SELECT
    playeruuid,
    effectname,
    usepoint,
    date
FROM
    donatedata
WHERE
    usepoint > 0;

CREATE TABLE IF NOT EXISTS donate_purchase_history(
    id INT AUTO_INCREMENT NOT NULL,
    uuid CHAR(36) NOT NULL,
    get_points INT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY(id, uuid)
);

INSERT INTO donate_purchase_history(
    uuid,
    get_points,
    timestamp
)
SELECT
    playeruuid,
    getpoint,
    date
FROM
    donatedata
WHERE
    getpoint > 0;