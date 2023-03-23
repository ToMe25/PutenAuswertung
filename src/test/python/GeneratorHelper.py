# A helper file containing methods to use for generating unit test classes.
import os
import os.path as path


def find_project_root() -> path:
    """Gets the project root to run relative to.
    
    The first checked path containing a "src" directory is considered the project root.
    First the current working directory is checked.
    Then its three parents.
    After that the directory in which the script is located is checked.
    Next are the three parents of it.
    If none of these contain a "src" directory this method returns the current working directory.
    
    Returns
    -------
    str
        The path to be used as the project root.
    """
    root = path.abspath(os.getcwd())
    if path.exists(path.join(root, 'src')):
        return root
    
    root = path.dirname(root)
    if path.exists(path.join(root, 'src')):
        return root
    
    root = path.dirname(root)
    if path.exists(path.join(root, 'src')):
        return root
    
    root = path.dirname(root)
    if path.exists(path.join(root, 'src')):
        return root
    
    root = path.dirname(path.realpath(__file__))
    if path.exists(path.join(root, 'src')):
        return root
    
    root = path.dirname(root)
    if path.exists(path.join(root, 'src')):
        return root
    
    root = path.dirname(root)
    if path.exists(path.join(root, 'src')):
        return root
    
    root = path.dirname(root)
    if path.exists(path.join(root, 'src')):
        return root
    
    return path.abspath(os.getcwd())
