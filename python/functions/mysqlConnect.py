import mysql.connector


def convertToBinaryData(filename):
    # Convert digital data to binary format
    with open(filename, 'rb') as file:
        binaryData = file.read()
    return binaryData


def convertToStringData(filename):
    # Convert digital data to binary format
    with open(filename, "r", encoding="utf-8") as file:
        stringData = file.read()
    return stringData


def insertBLOB(id, bfile, cfile):
    print("inserting bytes to table")
    try:
        connection = mysql.connector.connect(host='127.0.0.1',
                                             port='10001',
                                             database='253mysql',
                                             user='test',
                                             password='test')

        cursor = connection.cursor()
        sql_insert_blob_query = """ insert into blob_clob(id,b_blob,c_clob) VALUES (%s,%s,%s)"""

        b_data = convertToBinaryData(bfile)
        c_data = convertToStringData(cfile)

        # Convert data into tuple format
        insert_blob_tuple = (id, b_data, c_data)
        result = cursor.executemany(sql_insert_blob_query, insert_blob_tuple)
        connection.commit()
        print("inserted successfully to table", result)

    except mysql.connector.Error as error:
        print("Failed inserting to table {}".format(error))

    finally:
        if connection.is_connected():
            cursor.close()
            connection.close()
            print("MySQL connection is closed")


insertBLOB(8, "/home/yoloz/test/test.png", "/home/yoloz/test/test.txt")
